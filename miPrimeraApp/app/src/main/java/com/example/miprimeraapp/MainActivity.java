package com.example.miprimeraapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    DB db;
    Button btn;
    TextView tempval;
    String accion = "nuevo", idProducto = "", urlFoto1 = "", urlFoto2 = "", urlFoto3 = "";
    FloatingActionButton fab;

    // Array de imágenes para los 3 productos
    ImageView[] imgProductos = new ImageView[3];
    int imagenActual = 0;
    Intent tomarFotoIntento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar las 3 imágenes
        imgProductos[0] = findViewById(R.id.imgFotoProducto1);
        imgProductos[1] = findViewById(R.id.imgFotoProducto2);
        imgProductos[2] = findViewById(R.id.imgFotoProducto3);

        // Asignar listener a cada imagen
        for (int i = 0; i < imgProductos.length; i++) {
            final int index = i;
            imgProductos[i].setOnClickListener(v -> {
                imagenActual = index;
                tomarFoto();
            });
        }

        db = new DB(this);

        btn = findViewById(R.id.btnGuardarAmigo);
        btn.setOnClickListener(v -> guardarProducto());

        fab = findViewById(R.id.fabListaAmigo);
        fab.setOnClickListener(v -> regresarListaProductos());

        mostrarDatosProducto();
    }

    private void mostrarDatosProducto() {
        try {
            Bundle parametros = getIntent().getExtras();
            if (parametros != null) {
                accion = parametros.getString("accion");
                if (accion.equals("modificar")) {
                    JSONObject datos = new JSONObject(parametros.getString("producto"));
                    idProducto = datos.getString("idProducto");

                    tempval = findViewById(R.id.txtNombreAmigos);
                    tempval.setText(datos.getString("nombre"));

                    tempval = findViewById(R.id.txtDireccionAmigos);
                    tempval.setText(datos.getString("descripcion"));

                    tempval = findViewById(R.id.txtTelefonoAmigos);
                    tempval.setText(datos.getString("precio"));

                    tempval = findViewById(R.id.txtEmailAmigos);
                    tempval.setText(datos.getString("stock"));

                    tempval = findViewById(R.id.txtDuiAmigos);
                    tempval.setText(datos.getString("categoria"));

                    // Cargar las imágenes guardadas
                    cargarImagenesProducto(idProducto);
                }
            } else {
                // Si es nuevo producto, limpiar campos
                limpiarCampos();
            }
        } catch (Exception e) {
            mostrarMensaje("Error al mostrar los datos: " + e.getMessage());
        }
    }

    private void cargarImagenesProducto(String idProducto) {
        try {
            // Limpiar URLs actuales
            urlFoto1 = "";
            urlFoto2 = "";
            urlFoto3 = "";

            // Obtener las imágenes de la base de datos (igual que en lista_productos)
            Cursor cImagenes = db.obtener_imagenes(idProducto);

            int contador = 0;
            while (cImagenes.moveToNext() && contador < 3) {
                String urlFoto = cImagenes.getString(0); // obtener urlFoto
                int orden = cImagenes.getInt(1); // obtener orden

                // Guardar en la variable correspondiente según el orden
                switch (orden) {
                    case 0:
                        urlFoto1 = urlFoto;
                        break;
                    case 1:
                        urlFoto2 = urlFoto;
                        break;
                    case 2:
                        urlFoto3 = urlFoto;
                        break;
                }
                contador++;
            }
            cImagenes.close();

            // Mostrar las imágenes en los ImageView
            if (!urlFoto1.isEmpty()) {
                imgProductos[0].setImageURI(Uri.parse(urlFoto1));
            } else {
                imgProductos[0].setImageResource(R.drawable.camara);
            }

            if (!urlFoto2.isEmpty()) {
                imgProductos[1].setImageURI(Uri.parse(urlFoto2));
            } else {
                imgProductos[1].setImageResource(R.drawable.camara);
            }

            if (!urlFoto3.isEmpty()) {
                imgProductos[2].setImageURI(Uri.parse(urlFoto3));
            } else {
                imgProductos[2].setImageResource(R.drawable.camara);
            }

            mostrarMensaje("Imágenes cargadas: " +
                    (urlFoto1.isEmpty() ? "0" : "1") +
                    (urlFoto2.isEmpty() ? "" : ",2") +
                    (urlFoto3.isEmpty() ? "" : ",3"));

        } catch (Exception e) {
            mostrarMensaje("Error al cargar imágenes: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        idProducto = "";
        urlFoto1 = "";
        urlFoto2 = "";
        urlFoto3 = "";

        // Limpiar los TextViews
        TextView txtNombre = findViewById(R.id.txtNombreAmigos);
        txtNombre.setText("");

        TextView txtDireccion = findViewById(R.id.txtDireccionAmigos);
        txtDireccion.setText("");

        TextView txtTelefono = findViewById(R.id.txtTelefonoAmigos);
        txtTelefono.setText("");

        TextView txtEmail = findViewById(R.id.txtEmailAmigos);
        txtEmail.setText("");

        TextView txtDui = findViewById(R.id.txtDuiAmigos);
        txtDui.setText("");

        // Limpiar las imágenes
        for (int i = 0; i < imgProductos.length; i++) {
            imgProductos[i].setImageResource(R.drawable.camara);
        }
    }

    private void tomarFoto() {
        tomarFotoIntento = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File fotoProducto = null;

        try {
            fotoProducto = crearImgProducto();
            if (fotoProducto != null) {
                Uri uriFoto = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.miprimeraapp.fileprovider", fotoProducto);
                tomarFotoIntento.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
                startActivityForResult(tomarFotoIntento, 1);
            } else {
                mostrarMensaje("No se pudo crear la foto");
            }
        } catch (Exception e) {
            mostrarMensaje("Error al tomar la foto: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 1 && resultCode == RESULT_OK) {
                // Mostrar la foto en el ImageView correspondiente
                String urlFoto = obtenerUrlFoto(imagenActual);
                imgProductos[imagenActual].setImageURI(Uri.parse(urlFoto));
                mostrarMensaje("Foto " + (imagenActual + 1) + " guardada");
            } else {
                mostrarMensaje("Error al mostrar foto");
            }
        } catch (Exception e) {
            mostrarMensaje("Error al abrir camara: " + e.getMessage());
        }
    }

    private String obtenerUrlFoto(int index) {
        switch (index) {
            case 0: return urlFoto1;
            case 1: return urlFoto2;
            case 2: return urlFoto3;
            default: return "";
        }
    }

    private void guardarUrlFoto(int index, String url) {
        switch (index) {
            case 0: urlFoto1 = url; break;
            case 1: urlFoto2 = url; break;
            case 2: urlFoto3 = url; break;
        }
    }

    private File crearImgProducto() throws Exception {
        String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "producto_" + fechaHoraMs + "_" + imagenActual;
        File dirAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        if (!dirAlmacenamiento.exists()) {
            dirAlmacenamiento.mkdir();
        }
        File image = File.createTempFile(fileName, ".jpg", dirAlmacenamiento);
        guardarUrlFoto(imagenActual, image.getAbsolutePath());
        return image;
    }

    private void guardarProducto() {
        tempval = findViewById(R.id.txtNombreAmigos);
        String nombre = tempval.getText().toString();

        tempval = findViewById(R.id.txtDireccionAmigos);
        String descripcion = tempval.getText().toString();

        tempval = findViewById(R.id.txtTelefonoAmigos);
        String precio = tempval.getText().toString();

        tempval = findViewById(R.id.txtEmailAmigos);
        String stock = tempval.getText().toString();

        tempval = findViewById(R.id.txtDuiAmigos);
        String categoria = tempval.getText().toString();

        // Preparar array de imágenes
        String[] imagenes = {urlFoto1, urlFoto2, urlFoto3};
        String[] datos = {idProducto, nombre, descripcion, precio, stock, categoria};

        String respuesta = db.administrar_productos(accion, datos, imagenes);

        if (respuesta.equals("ok")) {
            mostrarMensaje("Producto guardado con éxito.");
            regresarListaProductos();
        } else {
            mostrarMensaje("Error: " + respuesta);
        }
    }

    private void mostrarMensaje(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void regresarListaProductos() {
        Intent intent = new Intent(this, lista_productos.class);
        startActivity(intent);
        finish();
    }
}