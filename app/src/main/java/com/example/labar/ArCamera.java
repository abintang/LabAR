package com.example.labar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.android.filament.Engine;
import com.google.android.filament.filamat.MaterialBuilder;
import com.google.android.filament.filamat.MaterialPackage;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ArCamera extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    // Deklarasi semua variable yang dibutuhkan pada activity camera
    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private boolean matrixDetected = false;
    private boolean rabbitDetected = false;
    private boolean deepDetected = false;
    private boolean visionDetected = false;
    private boolean ameDetected = false;
    private AugmentedImageDatabase database;
    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private MediaPlayer mediaPlayer;
    
    Button markerless, reset;

    CardView btn_bottomSheet;
    BottomSheetBehavior sheetBehavior;
    ConstraintLayout bottomSheet;
    ImageView arrowUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // mengatur view yang digunakan activity ini
        setContentView(R.layout.activity_ar_camera);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        // Inisialisasi variable yang telah di deklarasi dengan id masing-masing komponen.
        bottomSheet = findViewById(R.id.bottomSheet);
        btn_bottomSheet = findViewById(R.id.btn_up);
        sheetBehavior =  BottomSheetBehavior.from(bottomSheet); // Tidak Wajib
        arrowUp = findViewById(R.id.logo_up); // Tidak Wajib

        /* menggunakan method setoncliklistener pada variable btn_bottomsheet untuk mengaktifkan
        behavior bottomsheet. (Tidak Wajib)*/
        btn_bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    arrowUp.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);
                } else {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    arrowUp.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
                }
            }
        });


        // Deklarasi dan Inisialisasi Toolbar pada activity ini
        Toolbar mToolbar = (Toolbar) findViewById(R.id.markerAr);
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        reset = findViewById(R.id.btn_reset);
        markerless = findViewById(R.id.btn_markerless);

        // button untuk mereset activity
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArCamera.this, ArCamera.class);
                overridePendingTransition(0,0);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });

        // button untuk menuju activity markerless
        markerless.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArCamera.this, Markerless.class);
                overridePendingTransition(0,0);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });

        /* Bagian Wajib untuk dibuat dan inisialisasi fragment kamera pada activity_ar_camera.xml
        dan menyesuaikan dengan id yang telah di inisialisasi*/
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        if(Sceneform.isSupported(this)) {
           // memanggil kedua method 3d video dibawah apabila Support.
            loadMatrixModel();
            loadMatrixMaterial();
        }
    }

    // Override Method untuk attach fragment dengan id fragment yang ditentukan pada layout.
    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }


    // override method session configurasi
    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane deteksi
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Mengatur Autofocus kamera
        config.setFocusMode(Config.FocusMode.AUTO);
        session.configure(config);
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            throw new RuntimeException(e);
        }

        // Inisialisasi variable database
        database = new AugmentedImageDatabase(session);

        // Deklarasi dan Inisialisasi Bitmap dengan marker yang ditentukan pada drawable.
        Bitmap matrixImage = BitmapFactory.decodeResource(getResources(), R.drawable.markervideo);
        Bitmap rabbitImage = BitmapFactory.decodeResource(getResources(), R.drawable.markerrobot);
        Bitmap visionImage = BitmapFactory.decodeResource(getResources(), R.drawable.markervision);
        Bitmap deepImage = BitmapFactory.decodeResource(getResources(), R.drawable.markerdeep);
        Bitmap ameImage = BitmapFactory.decodeResource(getResources(), R.drawable.markercomp);

        // menambahkan variable diatas kedalam database dan masing-masing nama harus bersifat unique
        database.addImage("matrix", matrixImage);
        database.addImage("rabbit", rabbitImage);
        database.addImage("vision", visionImage);
        database.addImage("deep", deepImage);
        database.addImage("ame", ameImage);

        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    // Method untuk memanggil basis dari 3d model video pada folder assets (Tidak Wajib Apabila tidak mau menampilkan video).
    private void loadMatrixModel() {
        futures.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/Video.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    //removing shadows for this Renderable
                    model.setShadowCaster(false);
                    model.setShadowReceiver(true);
                    plainVideoModel = model;
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }

    // method untuk mengatur material 3d video melalui filament. (Tidak Wajib Apabila tidak mau menampilkan video).
    private void loadMatrixMaterial() {
        Engine filamentEngine = EngineInstance.getEngine().getFilamentEngine();

        MaterialBuilder.init();
        MaterialBuilder materialBuilder = new MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("External Video Material")
                .require(MaterialBuilder.VertexAttribute.UV0)
                .shading(MaterialBuilder.Shading.UNLIT)
                .doubleSided(true)
                .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_EXTERNAL, MaterialBuilder.SamplerFormat.FLOAT, MaterialBuilder.ParameterPrecision.DEFAULT, "videoTexture")
                .optimization(MaterialBuilder.Optimization.NONE);

        MaterialPackage plainVideoMaterialPackage = materialBuilder
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n")
                .build(filamentEngine);
        if (plainVideoMaterialPackage.isValid()) {
            ByteBuffer buffer = plainVideoMaterialPackage.getBuffer();
            futures.add(Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept(material -> {
                        plainVideoMaterial = material;
                    })
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG).show();
                                return null;
                            }));
        }
        MaterialBuilder.shutdown();
    }


    // Method untuk menampilkan 3d model jika berhasil tracking marker
    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (matrixDetected && rabbitDetected && deepDetected && visionDetected) {
            return;
        }

        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
        if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            if (augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {
                if (!matrixDetected && augmentedImage.getName().equals("matrix")) {
                    matrixDetected = true;
                    Toast.makeText(this, "Video tag detected", Toast.LENGTH_LONG).show();

                    // AnchorNode placed to the detected tag and set it to the real size of the tag
                    // This will cause deformation if your AR tag has different aspect ratio than your video
                    anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);

                    TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
                    // For some reason it is shown upside down so this will rotate it correctly
                    videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 180f));
                    videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0f, 0), 180f));
                    anchorNode.addChild(videoNode);

                    // Setting texture
                    ExternalTexture externalTexture = new ExternalTexture();
                    RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
                    renderableInstance.setMaterial(plainVideoMaterial);

                    // Setting MediaPLayer
                    renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);
                    mediaPlayer = MediaPlayer.create(this, R.raw.video); // inisialisasi video pada folder raw.
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setSurface(externalTexture.getSurface());
                    mediaPlayer.start();

                }
               /* Bagian menampilkan 3d object robot (atur bagian Uri.parse("models/bla,bla") sesuai
                dengan 3d yang kamu miliki */
                if (!rabbitDetected && augmentedImage.getName().equals("rabbit")) {
                    rabbitDetected = true;
                    Toast.makeText(this, "Machine Learning tag detected", Toast.LENGTH_LONG).show();

                    anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);


                    futures.add(ModelRenderable.builder()
                            .setSource(this, Uri.parse("models/robot.glb"))
                            .setIsFilamentGltf(true)
                            .setAsyncLoadEnabled(true)
                            .build()
                            .thenAccept(rabbitModel -> {
                                modelNode.setRenderable(rabbitModel);
                                anchorNode.addChild(modelNode);
                            })
                            .exceptionally(
                                    throwable -> {
                                        Toast.makeText(this, "Unable to load Machine Learning model", Toast.LENGTH_LONG).show();
                                        return null;
                                    }));


                }

                // Bagian menampilkan 3d computer vision
                if (!visionDetected && augmentedImage.getName().equals("vision")) {
                    visionDetected = true;
                    Toast.makeText(this, "Computer Vision tag detected", Toast.LENGTH_LONG).show();

                    anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);


                    futures.add(ModelRenderable.builder()
                            .setSource(this, Uri.parse("models/vision.glb"))
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(visionModel -> {
                                anchorNode.removeChild(modelNode);
                                modelNode.setRenderable(visionModel);
                                anchorNode.addChild(modelNode);
                            })
                            .exceptionally(
                                    throwable -> {
                                        Toast.makeText(this, "Unable to load Computer Vision model", Toast.LENGTH_LONG).show();
                                        return null;
                                    }));


                }

                // Bagian menampilkan 3d computer deepLearning
                if (!deepDetected && augmentedImage.getName().equals("deep")) {
                    deepDetected = true;
                    Toast.makeText(this, "Deep Learning tag detected", Toast.LENGTH_LONG).show();

                    anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);


                    futures.add(ModelRenderable.builder()
                            .setSource(this, Uri.parse("models/deeplearning.glb"))
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(deepModel -> {
                                modelNode.setRenderable(deepModel);
                                anchorNode.addChild(modelNode);
                            })
                            .exceptionally(
                                    throwable -> {
                                        Toast.makeText(this, "Unable to load Deep Learning model", Toast.LENGTH_LONG).show();
                                        return null;
                                    }));


                }

                // Bagian menampilkan 3d computer
                if (!ameDetected && augmentedImage.getName().equals("ame")) {
                    ameDetected = true;
                    Toast.makeText(this, "Ame Computer tag detected", Toast.LENGTH_LONG).show();

                    anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);

                    futures.add(ModelRenderable.builder()
                            .setSource(this, Uri.parse("models/comp.glb"))
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(ameModel -> {
                                modelNode.setRenderable(ameModel)
                                        .animate(true).start();
                                anchorNode.addChild(modelNode);
                            })
                            .exceptionally(
                                    throwable -> {
                                        Toast.makeText(this, "Unable to load Deep Learning model", Toast.LENGTH_LONG).show();
                                        return null;
                                    }));
                }
            }
        }

        if (matrixDetected && rabbitDetected && visionDetected && deepDetected && ameDetected) {
            arFragment.getInstructionsController().setEnabled(
                    InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }
    }


}