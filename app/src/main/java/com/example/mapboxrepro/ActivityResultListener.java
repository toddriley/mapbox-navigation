package com.example.mapboxrepro;

import android.content.Intent;

public interface ActivityResultListener {
    void onCustomActivityResult(int requestCode, int resultCode, Intent bundle);
}
