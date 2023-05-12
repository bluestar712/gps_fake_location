package com.mock.gpssearch;

public class Note {
    int _id;
    String name_value;
    String lati_value;
    String longi_value;

    public Note(int _id, String name, String lati, String longi){
        this._id = _id;
        this.name_value = name;
        this.lati_value = lati;
        this.longi_value = longi;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getName_value() {
        return name_value;
    }

    public void setName_value(String name) {
        this.name_value = name;
    }

    public String getLati_value() {
        return lati_value;
    }

    public void setLati_value(String lati) {
        this.lati_value = lati;
    }

    public String getLongi_value() {
        return longi_value;
    }

    public void setLongi_value(String longi) {
        this.longi_value = longi;
    }

}
