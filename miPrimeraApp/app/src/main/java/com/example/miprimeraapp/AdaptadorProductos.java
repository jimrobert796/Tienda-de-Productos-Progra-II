package com.example.miprimeraapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class AdaptadorProductos extends BaseAdapter {
    Context context;
    ArrayList<Producto> alProductos;
    LayoutInflater inflater;

    public AdaptadorProductos(Context context, ArrayList<Producto> alProductos) {
        this.context = context;
        this.alProductos = alProductos;
    }

    @Override
    public int getCount() {
        return alProductos.size();
    }

    @Override
    public Object getItem(int position) {
        return alProductos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.fotos, parent, false);

        try {
            Producto producto = alProductos.get(position);

            TextView tempVal = itemView.findViewById(R.id.lblNombreAdaptador);
            tempVal.setText(producto.getNombre());

            tempVal = itemView.findViewById(R.id.lblTelefonoAdaptador);
            tempVal.setText("$" + String.format("%.2f", producto.getPrecio()) + " | Stock: " + producto.getStock());

            tempVal = itemView.findViewById(R.id.lblEmailAdaptador);
            tempVal.setText(producto.getCategoria());

            ImageView img = itemView.findViewById(R.id.imgFotoAdaptador);
            String fotoPrincipal = producto.getImagenPrincipal();
            if (fotoPrincipal != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(fotoPrincipal);
                img.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return itemView;
    }
}