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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_PICK_IMAGE = 2;

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
                mostrarOpcionesImagen();
            });
        }

        db = new DB(this);

        btn = findViewById(R.id.btnGuardarAmigo);
        btn.setOnClickListener(v -> guardarProducto());

        fab = findViewById(R.id.fabListaAmigo);
        fab.setOnClickListener(v -> regresarListaProductos());

        mostrarDatosProducto();

        // SEGUn esta monda es pa usar hacia atras Nolose rick Metodos del orto
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                regresarListaProductos();
            }
        });


    }



    private void mostrarOpcionesImagen() {
        String[] opciones = {"📷 Tomar foto con cámara", "🖼️ Seleccionar de galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen para producto");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                tomarFoto(); // Opción cámara
            } else {
                seleccionarDeGaleria(); // Opción galería
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    // 📷 TOMAR FOTO CON CÁMARA - Usando el mismo código que funcionaba en amigos
    private void tomarFoto() {
        tomarFotoIntento = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File fotoProducto = null;

        try {
            fotoProducto = crearImgProducto();
            if (fotoProducto != null) {
                Uri uriFoto = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.miprimeraapp.fileprovider", fotoProducto);
                tomarFotoIntento.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
                startActivityForResult(tomarFotoIntento, REQUEST_TAKE_PHOTO);
            } else {
                mostrarMensaje("No se pudo crear la foto");
            }
        } catch (Exception e) {
            mostrarMensaje("Error al tomar la foto: " + e.getMessage());
        }
    }

    // 🖼️ SELECCIONAR DE GALERÍA
    private void seleccionarDeGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // 🔧 CREAR ARCHIVO PARA LA FOTO (mismo código que funcionaba)
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

    // 💾 GUARDAR IMAGEN DE GALERÍA
    private String guardarImagenDesdeGaleria(Uri uri) {
        try {
            String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "producto_galeria_" + fechaHoraMs + "_" + imagenActual + ".jpg";
            File dirAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_DCIM);
            if (!dirAlmacenamiento.exists()) {
                dirAlmacenamiento.mkdir();
            }
            File imageFile = new File(dirAlmacenamiento, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            mostrarMensaje("Error al guardar imagen: " + e.getMessage());
            return "";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
                // Foto tomada con cámara
                String urlFoto = obtenerUrlFoto(imagenActual);
                if (!urlFoto.isEmpty()) {
                    imgProductos[imagenActual].setImageURI(Uri.parse(urlFoto));
                    mostrarMensaje("📷 Foto " + (imagenActual + 1) + " guardada");
                } else {
                    mostrarMensaje("Error: No se pudo obtener la URL de la foto");
                }
            }
            else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
                // Imagen seleccionada de galería
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    String nuevaUrl = guardarImagenDesdeGaleria(selectedImageUri);
                    if (!nuevaUrl.isEmpty()) {
                        guardarUrlFoto(imagenActual, nuevaUrl);
                        imgProductos[imagenActual].setImageURI(Uri.parse(nuevaUrl));
                        mostrarMensaje("🖼️ Imagen " + (imagenActual + 1) + " seleccionada");
                    } else {
                        mostrarMensaje("Error al guardar la imagen");
                    }
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error: " + e.getMessage());
        }
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

                    cargarImagenesProducto(idProducto);
                }
            } else {
                limpiarCampos();
            }
        } catch (Exception e) {
            mostrarMensaje("Error al mostrar los datos: " + e.getMessage());
        }
    }

    private void cargarImagenesProducto(String idProducto) {
        try {
            urlFoto1 = "";
            urlFoto2 = "";
            urlFoto3 = "";

            Cursor cImagenes = db.obtener_imagenes(idProducto);

            int contador = 0;
            while (cImagenes.moveToNext() && contador < 3) {
                String urlFoto = cImagenes.getString(0);
                int orden = cImagenes.getInt(1);

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

        } catch (Exception e) {
            mostrarMensaje("Error al cargar imágenes: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        idProducto = "";
        urlFoto1 = "";
        urlFoto2 = "";
        urlFoto3 = "";

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

        for (int i = 0; i < imgProductos.length; i++) {
            imgProductos[i].setImageResource(R.drawable.camara);
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
    private void guardarProducto() {
        try {
            // Obtener valores de los campos
            tempval = findViewById(R.id.txtNombreAmigos);
            String nombre = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtDireccionAmigos);
            String descripcion = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtTelefonoAmigos);
            String precio = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtEmailAmigos);
            String stock = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtDuiAmigos);
            String categoria = tempval.getText().toString().trim();

            // VALIDACIONES

            // Validar campo NOMBRE
            if (nombre.isEmpty()) {
                mostrarMensaje("Por favor ingrese el nombre del producto");
                findViewById(R.id.txtNombreAmigos).requestFocus();
                return;
            }


            // Validar campo DESCRIPCIÓN
            if (descripcion.isEmpty()) {
                mostrarMensaje("Por favor ingrese la descripción del producto");
                findViewById(R.id.txtDireccionAmigos).requestFocus();
                return;
            }


            // Validar campo PRECIO
            if (precio.isEmpty()) {
                mostrarMensaje("Por favor ingrese el precio del producto");
                findViewById(R.id.txtTelefonoAmigos).requestFocus();
                return;
            }

            // Validar que el precio sea un número válido y mayor a 0
            double precioDouble;
            try {
                precioDouble = Double.parseDouble(precio);
                if (precioDouble <= 0) {
                    mostrarMensaje("El precio debe ser mayor a 0");
                    findViewById(R.id.txtTelefonoAmigos).requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("El precio debe ser un número válido");
                findViewById(R.id.txtTelefonoAmigos).requestFocus();
                return;
            }

            // Validar campo STOCK
            if (stock.isEmpty()) {
                mostrarMensaje("Por favor ingrese el stock del producto");
                findViewById(R.id.txtEmailAmigos).requestFocus();
                return;
            }

            // Validar que el stock sea un número entero válido y mayor o igual a 0
            int stockInt;
            try {
                stockInt = Integer.parseInt(stock);
                if (stockInt < 0) {
                    mostrarMensaje("El stock no puede ser negativo");
                    findViewById(R.id.txtEmailAmigos).requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("El stock debe ser un número entero válido");
                findViewById(R.id.txtEmailAmigos).requestFocus();
                return;
            }

            // Validar campo CATEGORÍA (opcional pero recomendado)
            if (categoria.isEmpty()) {
                mostrarMensaje("Por favor ingrese la categoría del producto");
                findViewById(R.id.txtDuiAmigos).requestFocus();
                return;
            }

            // Validar que al menos tenga una imagen (opcional - si es requerida)
            int imagenesValidas = 0;
            if (urlFoto1 != null && !urlFoto1.isEmpty()) imagenesValidas++;
            if (urlFoto2 != null && !urlFoto2.isEmpty()) imagenesValidas++;
            if (urlFoto3 != null && !urlFoto3.isEmpty()) imagenesValidas++;

            if (imagenesValidas == 0) {
                mostrarMensaje("Por favor agregue al menos una imagen del producto");
                return;
            }

            // GUARDAR PRODUCTO
            String[] imagenes = {urlFoto1, urlFoto2, urlFoto3};
            String[] datos = {idProducto, nombre, descripcion, precio, stock, categoria};

            String respuesta = db.administrar_productos(accion, datos, imagenes);

            if (respuesta.equals("ok")) {
                mostrarMensaje("Producto guardado con éxito.");
                regresarListaProductos();
            } else {
                mostrarMensaje("Error al guardar: " + respuesta);
            }

        } catch (NumberFormatException e) {
            mostrarMensaje("Error: Precio y Stock deben ser números válidos");
        } catch (Exception e) {
            mostrarMensaje("Error al guardar: " + e.getMessage());
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