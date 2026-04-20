package com.example.miprimeraapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    FloatingActionButton fab;
    Bundle parametros = new Bundle();
    DB db;
    ListView ltsProductos;
    Cursor cProductos;
    final ArrayList<Producto> alProductos = new ArrayList<>();
    final ArrayList<Producto> alProductosCopia = new ArrayList<>();
    JSONArray jsonArray;
    int posicion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_amigos);

        parametros.putString("accion", "nuevo");
        db = new DB(this);

        fab = findViewById(R.id.fabAgregarAmigos);
        fab.setOnClickListener(v -> abrirActivity());

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

    private void borrarProducto() {
        try {
            String nombre = jsonArray.getJSONObject(posicion).getString("nombre");
            AlertDialog.Builder confirmacion = new AlertDialog.Builder(this);
            confirmacion.setTitle("¿Está seguro de borrar?");
            confirmacion.setMessage(nombre);
            confirmacion.setPositiveButton("SÍ", (dialog, which) -> {
                try {
                    String respuesta = db.administrar_productos("eliminar",
                            new String[]{jsonArray.getJSONObject(posicion).getString("idProducto")},
                            new String[]{});
                    if (respuesta.equals("ok")) {
                        obtenerProductos();
                        mostrarMsg("Producto borrado con éxito.");
                    }
                } catch (Exception e) {
                    mostrarMsg(e.getMessage());
                }
            });
            confirmacion.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            confirmacion.create().show();
        } catch (Exception e) {
            mostrarMsg("Error al borrar el producto: " + e.getMessage());
        }
    }

    private void buscarProductos() {
        TextView tempval = findViewById(R.id.txtBuscarAmigos);
        tempval.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alProductos.clear();
                String buscar = tempval.getText().toString().trim().toLowerCase();
                if (buscar.isEmpty()) {
                    alProductos.addAll(alProductosCopia);
                } else {
                    for (Producto item : alProductosCopia) {
                        if (item.getNombre().toLowerCase().contains(buscar) ||
                                item.getCategoria().toLowerCase().contains(buscar)) {
                            alProductos.add(item);
                        }
                    }
                }
                ltsProductos.setAdapter(new AdaptadorProductos(getApplicationContext(), alProductos));
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
            cProductos = db.lista_productos();
            if (cProductos.moveToFirst()) {
                jsonArray = new JSONArray();
                do {
                    JSONObject jsonObject = new JSONObject();
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
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    // Obtener las imágenes del producto
                    Cursor cImagenes = db.obtener_imagenes(jsonObject.getString("idProducto"));
                    ArrayList<String> imagenes = new ArrayList<>();
                    while (cImagenes.moveToNext()) {
                        imagenes.add(cImagenes.getString(0));
                    }
                    cImagenes.close();

                    Producto producto = new Producto(
                            jsonObject.getString("idProducto"),
                            jsonObject.getString("nombre"),
                            jsonObject.getString("descripcion"),
                            jsonObject.getDouble("precio"),
                            jsonObject.getInt("stock"),
                            jsonObject.getString("categoria"),
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