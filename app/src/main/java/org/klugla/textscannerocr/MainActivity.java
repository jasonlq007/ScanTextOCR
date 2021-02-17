package org.klugla.textscannerocr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 0;
    private static final int REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUESTS = 0;
    StringBuilder detectedText = new StringBuilder();
    static int sectionLength=0;

    private static final String TAG = MainActivity.class.getSimpleName();

    private Uri imageUri;
    private TextView detectedTextView;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUESTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // FIXME: Handle this case the user denied to grant the permissions
                }
                break;
            }
            default:
                // TODO: Take care of this case later
                break;
        }
    }
    private void captureImage()
    {
        String filename = System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void requestPermissions()
    {
        List<String> requiredPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA);
        }

        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    requiredPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUESTS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();

        findViewById(R.id.btnExportToPdf).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (detectedText.toString().length()>0)
              {
                  exportPDF(detectedText.toString());
              }
              else
              {
                  Toast.makeText(getApplicationContext(),"Please capture a photo or choose an image from gallery",Toast.LENGTH_SHORT).show();

              }

            }
        });
        findViewById(R.id.takeNextPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               imageMode();
                if (detectedText.toString().length()>0)
                    findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);
                else
                    findViewById(R.id.btnExportToPdf).setVisibility(View.INVISIBLE);
            }
        });

        findViewById(R.id.repeat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Log.v("JSON1",String.valueOf(sectionLength));
            Log.v("JSON2",String.valueOf(detectedText));
            String sectionHolder=  detectedText.substring(0,detectedText.length()-sectionLength);
            detectedText.setLength(0);
            detectedText.append(sectionHolder);
            detectedTextView.setText(detectedText);
            captureImage();
            }
        });

        findViewById(R.id.finishBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageMode();
                if (detectedText.toString().length()>0)
                    findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);
                else
                    findViewById(R.id.btnExportToPdf).setVisibility(View.INVISIBLE);


            }
        });
        findViewById(R.id.take_a_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
                optionMode();
                if (detectedText.toString().length()>0)
                    findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);

            }
        });
        findViewById(R.id.btnGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_GALLERY);
                optionMode();
                if (detectedText.toString().length()>0)
                    findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);
                else
                    findViewById(R.id.btnExportToPdf).setVisibility(View.INVISIBLE);
            }
        });

        findViewById(R.id.cancel_option).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectedTextView.setText("");
                detectedText.setLength(0);
                imageMode();
                if (detectedText.toString().length()>0)
                    findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);
                else
                    findViewById(R.id.btnExportToPdf).setVisibility(View.INVISIBLE);
            }
        });


        detectedTextView = (TextView) findViewById(R.id.detected_text);
        detectedTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void inspectFromBitmap(Bitmap bitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        try {
            if (!textRecognizer.isOperational()) {
                new AlertDialog.
                        Builder(this).
                        setMessage("Text recognizer could not be set up on your device").show();
                return;
            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> origTextBlocks = textRecognizer.detect(frame);
            List<TextBlock> textBlocks = new ArrayList<>();
            for (int i = 0; i < origTextBlocks.size(); i++) {
                TextBlock textBlock = origTextBlocks.valueAt(i);
                textBlocks.add(textBlock);
            }
            Collections.sort(textBlocks, new Comparator<TextBlock>() {
                @Override
                public int compare(TextBlock o1, TextBlock o2) {
                    int diffOfTops = o1.getBoundingBox().top - o2.getBoundingBox().top;
                    int diffOfLefts = o1.getBoundingBox().left - o2.getBoundingBox().left;
                    if (diffOfTops != 0) {
                        return diffOfTops;
                    }
                    return diffOfLefts;
                }
            });

            for (TextBlock textBlock : textBlocks) {
                if (textBlock != null && textBlock.getValue() != null) {
                    detectedText.append(textBlock.getValue());
                    sectionLength=textBlock.getValue().toString().length();
                    detectedText.append("\n");
                }
            }

            detectedTextView.setText(detectedText);
        }
        finally {
            textRecognizer.release();
        }
    }
    private void imageMode()
    {
        findViewById(R.id.takeNextPhoto).setVisibility(View.INVISIBLE);
        findViewById(R.id.finishBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.repeat).setVisibility(View.INVISIBLE);
        findViewById(R.id.cancel_option).setVisibility(View.INVISIBLE);
        findViewById(R.id.take_a_photo).setVisibility(View.VISIBLE);
        findViewById(R.id.btnGallery).setVisibility(View.VISIBLE);
        findViewById(R.id.btnExportToPdf).setVisibility(View.VISIBLE);
    }

    private void optionMode()
    {
        findViewById(R.id.takeNextPhoto).setVisibility(View.VISIBLE);
        findViewById(R.id.finishBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.repeat).setVisibility(View.VISIBLE);
        findViewById(R.id.cancel_option).setVisibility(View.VISIBLE);
        findViewById(R.id.take_a_photo).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnGallery).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnExportToPdf).setVisibility(View.INVISIBLE);
    }

    private void exportPDF(String detectedText)
    {
       // String fpath = "/sdcard/" + "ocr" + ".pdf";
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
        String format = s.format(new Date());
        String fpath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/"+format+".pdf";
        File file = new File(fpath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // To customise the text of the pdf
        // we can use FontFamily
        Font bfBold12 = new Font(Font.FontFamily.TIMES_ROMAN,
                12, Font.BOLD, new BaseColor(0, 0, 0));
        Font bf12 = new Font(Font.FontFamily.TIMES_ROMAN,
                12);
        // create an instance of itext document
        Document document = new Document();
        try {
            PdfWriter.getInstance(document,
                    new FileOutputStream(file.getAbsoluteFile()));
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        document.open();
        //using add method in document to insert a paragraph
        try {
            document.add(new Paragraph(detectedText));
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        // close document
        document.close();
        Toast.makeText(getApplicationContext(),"Saved to"+" "+file.toString(),Toast.LENGTH_SHORT).show();
        openPDFIntent(file);
    }

    private void openPDFIntent(File file)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkURI = FileProvider.getUriForFile(
                getApplicationContext(),
                getApplicationContext()
                        .getPackageName() + ".provider", file);
        intent.setDataAndType(apkURI, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void inspect(Uri uri) {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 2;
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
            bitmap = BitmapFactory.decodeStream(is, null, options);
            inspectFromBitmap(bitmap);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to find the file: " + uri, e);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close InputStream", e);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    inspect(data.getData());
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (imageUri != null) {
                        inspect(imageUri);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
