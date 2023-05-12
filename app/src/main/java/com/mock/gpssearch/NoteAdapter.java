package com.mock.gpssearch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> implements OnPersonItemClickListener {
    private static final String TAG = "NoteAdapter";

    ArrayList<Note> items = new ArrayList<>();
    OnPersonItemClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.fragement_event_item, parent, false);


        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note item = items.get(position);
        holder.setItem(item);
        holder.setLayout();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onItemClick(ViewHolder holder, View view, int position) {
        if(listener != null){
            listener.onItemClick(holder, view, position);
        }
    }

    public void setOnItemClicklistener(OnPersonItemClickListener listener){
        this.listener = listener;
    }



    class ViewHolder extends RecyclerView.ViewHolder{

        LinearLayout layoutTodo;
        TextView tx_name, tx_lati, tx_longi;
        Button deleteButton;

        public ViewHolder(View itemView, final OnPersonItemClickListener listener){
            super(itemView);

            layoutTodo = itemView.findViewById(R.id.layoutTodo);
            tx_name = itemView.findViewById(R.id.tx_name);
            tx_lati = itemView.findViewById(R.id.tx_lati);
            tx_longi = itemView.findViewById(R.id.tx_longi);
            deleteButton = itemView.findViewById(R.id.deleteButton);


            deleteButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {


                    String name = tx_name.getText().toString();
                    deleteToDo(name);
                    Toast.makeText(v.getContext(),"it's deleted!.",Toast.LENGTH_SHORT).show();
                    removeItem(getAdapterPosition());
                }


                Context context;

                private void deleteToDo(String TODO){

                    String deleteSql = "delete from " + NoteDatabase.TABLE_NOTE + " where " + "  name = '" + TODO+"'";
                    NoteDatabase database = NoteDatabase.getInstance(context);

                    database.execSQL(deleteSql);
                }
            });


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if(listener != null){
                        listener.onItemClick(ViewHolder.this, v, pos);
                    }
                }
            });

        }

        public void setItem(Note item){
            tx_name.setText(item.getName_value());
            tx_lati.setText(item.getLati_value());
            tx_longi.setText(item.getLongi_value());
        }



        public void setLayout(){
            layoutTodo.setVisibility(View.VISIBLE);
        }
    }


    public void setItems(ArrayList<Note> items){
        this.items = items;
    }

    public void removeItem(int position){
        items.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }


}
