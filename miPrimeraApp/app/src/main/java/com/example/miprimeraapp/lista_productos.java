package com.example.miprimeraapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class lista_productos extends AppCompatActivity {

    Bundle parametros = new Bundle();
    DB db;
    FloatingActionButton fab;
    ListView ltsProductos;
    Cursor cProductos;
    final ArrayList<Producto> alProductos = new ArrayList<>();
    final ArrayList<Producto> alProductosCopia = new ArrayList<>();
    JSONArray jsonArray;
    JSONObject jsonObject;
    int posicion = 0;
    Producto misProducto;
    detectarInternet di;
    obtenerDatosServidor datosServidor;
    Producto productoSeleccionado;  // ← Agrega esta línea

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_amigos);

        parametros.putString("accion", "nuevo");
        db = new DB(this);

        fab = findViewById(R.id.fabAgregarAmigos);
        fab.setOnClickListener(v -> abrirActivity());

        di = new detectarInternet(this);

        obtenerProductos();
        buscarProductos();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mimenu, menu);

        try {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = info.position; // 🔥 IMPORTANTE: guardar la posición
            Producto productoSeleccionado = alProductos.get(posicion);
            menu.setHeaderTitle(productoSeleccionado.getNombre());

        } catch (Exception e) {
            mostrarMsg("Error al mostrar menú: " + e.getMessage());
        }
    }

// BUG SOLUCIONADO A LA HORA DE FILTRAR

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {
            Producto productoSeleccionado = alProductos.get(posicion);

            if (item.getItemId() == R.id.mnxAgregar) {
                parametros.putString("accion", "nuevo");
                abrirActivity();

            } else if (item.getItemId() == R.id.mnxModificar) {
                parametros.putString("accion", "modificar");

                // Convertir el producto seleccionado a JSON
                JSONObject jsonProducto = new JSONObject();
                jsonProducto.put("idProducto", productoSeleccionado.getIdProducto());
                jsonProducto.put("nombre", productoSeleccionado.getNombre());
                jsonProducto.put("descripcion", productoSeleccionado.getDescripcion());
                jsonProducto.put("precio", productoSeleccionado.getPrecio());
                jsonProducto.put("stock", productoSeleccionado.getStock());
                jsonProducto.put("categoria", productoSeleccionado.getCategoria());

                // 🔥 AGREGAR: Buscar _id y _rev para CouchDB
                try {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject temp = jsonArray.getJSONObject(i);
                        JSONObject docOriginal;

                        // Verificar si es CouchDB (tiene "value") o local
                        if (temp.has("value")) {
                            docOriginal = temp.getJSONObject("value");
                        } else {
                            docOriginal = temp;
                        }

                        // Comparar por idProducto
                        if (docOriginal.getString("idProducto").equals(productoSeleccionado.getIdProducto())) {

                            if (docOriginal.has("_id")) {
                                jsonProducto.put("_id", docOriginal.getString("_id"));
                                jsonProducto.put("_rev", docOriginal.getString("_rev"));
                            }

                            // 🔥 FIX IMÁGENES
                            if (docOriginal.has("imagenes")) {
                                jsonProducto.put("imagenes", docOriginal.getJSONArray("imagenes"));
                            }

                            break;
                        }
                    }
                } catch (Exception e) {
                    // Si no encuentra _id y _rev, solo es producto local, no hay problema
                    Log.d("DEBUG", "Producto local sin _id/_rev");
                }

                parametros.putString("producto", jsonProducto.toString());
                abrirActivity();

            } else if (item.getItemId() == R.id.mnxEliminar) {
                borrarProducto();
            }

            return true;

        } catch (Exception e) {
            mostrarMsg("Error al seleccionar item de menú: " + e.getMessage());
            return super.onContextItemSelected(item);
        }
    }

    private JSONObject getDoc(int pos) throws Exception {
        try {
            return jsonArray.getJSONObject(pos).getJSONObject("value"); // online
        } catch (Exception e) {
            return jsonArray.getJSONObject(pos); // offline
        }
    }

    private void borrarProducto() {
        try {
            JSONObject doc = getDoc(posicion);
            String nombre = doc.getString("nombre");

            AlertDialog.Builder confirmacion = new AlertDialog.Builder(this);
            confirmacion.setTitle("¿Está seguro de borrar?");
            confirmacion.setMessage(nombre);

            confirmacion.setPositiveButton("SÍ", (dialog, which) -> {
                try {

                    // 🔥 eliminar local
                    String respuesta = db.administrar_productos("eliminar",
                            new String[]{doc.getString("idProducto")},
                            new String[]{});

                    // 🔥 eliminar servidor SOLO si existe _id y _rev
                    if (respuesta.equals("ok") && di.hayConexionInternet()
                            && doc.has("_id") && doc.has("_rev")) {

                        JSONObject datosProducto = new JSONObject();

                        String _id = doc.getString("_id");
                        String _rev = doc.getString("_rev");

                        String url = utilidades.url_mantenimiento + "/" + _id + "?rev=" + _rev;

                        enviarDatosServidor obj = new enviarDatosServidor(this);
                        String resp = obj.execute(datosProducto.toString(), "DELETE", url).get();

                        JSONObject respuestaJSON = new JSONObject(resp);

                        if (!respuestaJSON.getBoolean("ok")) {
                            mostrarMsg("Error servidor: " + resp);
                        }
                    }

                    if (respuesta.equals("ok")) {
                        mostrarMsg("Producto eliminado correctamente");
                    }

                    obtenerProductos();

                } catch (Exception e) {
                    mostrarMsg(e.getMessage());
                }
            });

            confirmacion.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            confirmacion.create().show();

        } catch (Exception e) {
            mostrarMsg("Error al borrar: " + e.getMessage());
        }
    }

    private void buscarProductos() {
        TextView tempVal = findViewById(R.id.txtBuscarAmigos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alProductos.clear();
                String buscar = tempVal.getText().toString().trim().toLowerCase();
                if (buscar.length() <= 0) {
                    alProductos.addAll(alProductosCopia);
                } else {
                    for (Producto item : alProductosCopia) {
                        if (item.getNombre().toLowerCase().contains(buscar) ||
                                item.getCategoria().toLowerCase().contains(buscar)) {
                            alProductos.add(item);
                        }
                    }
                    ltsProductos.setAdapter(new AdaptadorProductos(getApplicationContext(), alProductos));
                }
            }
        });
    }

    private void abrirActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtras(parametros);
        startActivity(intent);
    }

    private void obtenerProductos() {
        try {
            if (di.hayConexionInternet()) { //si hay conexion a internet
                datosServidor = new obtenerDatosServidor();
                String respuesta = datosServidor.execute().get();
                jsonObject = new JSONObject(respuesta);
                jsonArray = jsonObject.getJSONArray("rows");
                mostrarProductos();
            } else { //no hay conexion a internet
                cProductos = db.lista_productos();
                if (cProductos.moveToFirst()) {
                    jsonArray = new JSONArray();
                    do {
                        jsonObject = new JSONObject();
                        jsonObject.put("idProducto", cProductos.getString(0));
                        jsonObject.put("nombre", cProductos.getString(1));
                        jsonObject.put("descripcion", cProductos.getString(2));
                        jsonObject.put("precio", cProductos.getDouble(3));
                        jsonObject.put("stock", cProductos.getInt(4));
                        jsonObject.put("categoria", cProductos.getString(5));
                        jsonArray.put(jsonObject);
                    } while (cProductos.moveToNext());
                    mostrarProductos();
                } else {
                    mostrarMsg("No hay productos que mostrar");
                    abrirActivity();
                }
            }
        } catch (Exception e) {
            mostrarMsg(e.getMessage());
        }
    }

    private void mostrarProductos() {
        try {
            if (jsonArray.length() > 0) {
                ltsProductos = findViewById(R.id.ltsAmigos);
                alProductos.clear();
                alProductosCopia.clear();

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject doc;
                    try {
                        doc = jsonArray.getJSONObject(i).getJSONObject("value"); // online
                    } catch (Exception e) {
                        doc = jsonArray.getJSONObject(i); // offline
                    }

                    ArrayList<String> imagenes = new ArrayList<>();

                    // ========= SI VIENE DE COUCHDB =========
                    if (doc.has("imagenes")) {
                        JSONArray imgs = doc.getJSONArray("imagenes");

                        for (int j = 0; j < imgs.length(); j++) {
                            JSONObject img = imgs.getJSONObject(j);
                            String url = img.getString("url");

                            if (!url.isEmpty()) {
                                imagenes.add(url);
                            }
                        }
                    }

                    // ========= SI NO HAY EN COUCHDB, BUSCAR LOCAL =========
                    if (imagenes.isEmpty()) {
                        Cursor cImagenes = db.obtener_imagenes(doc.getString("idProducto"));

                        while (cImagenes.moveToNext()) {
                            imagenes.add(cImagenes.getString(0));
                        }

                        cImagenes.close();
                    }

                    Producto producto = new Producto(
                            doc.getString("idProducto"),
                            doc.getString("nombre"),
                            doc.getString("descripcion"),
                            doc.getDouble("precio"),
                            doc.getInt("stock"),
                            doc.getString("categoria"),
                            imagenes
                    );

                    alProductos.add(producto);
                }

                alProductosCopia.addAll(alProductos);
                ltsProductos.setAdapter(new AdaptadorProductos(this, alProductos));
                registerForContextMenu(ltsProductos);

            } else {
                mostrarMsg("No hay productos que mostrar");
                abrirActivity();
            }

        } catch (Exception e) {
            mostrarMsg(e.getMessage());
        }
    }
    private void mostrarMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}