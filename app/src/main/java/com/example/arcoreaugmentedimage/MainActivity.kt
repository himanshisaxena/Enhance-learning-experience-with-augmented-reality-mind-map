@file:Suppress("UNREACHABLE_CODE")

package com.example.arcoreaugmentedimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.arcoreaugmentedimage.base.BaseCustomDialog
import com.example.arcoreaugmentedimage.databinding.DialogAddTextBinding
import com.example.arcoreaugmentedimage.databinding.DialogChooseCameraGalleryBinding
import com.example.arcoreaugmentedimage.models.NodeModel
import com.example.arcoreaugmentedimage.util.*
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import me.jagar.mindmappingandroidlibrary.Views.Item
import me.jagar.mindmappingandroidlibrary.Views.ItemLocation
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture


class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein by kodein()

    private val mPrefProvider: PreferenceProvider by instance()

    private val TAG = MainActivity::class.java.simpleName
    private var installRequested: Boolean = false
    private var session: Session? = null
    private var shouldConfigureSession = false
    private val messageSnackBarHelper = SnackbarHelper()
    private var sensorsMap = HashMap<String, ViewRenderable>()
    private var mParentItem: Item? = null
    private var mChooseOptionDialog: BaseCustomDialog<DialogChooseCameraGalleryBinding>? = null
    private var mParentAdded: Boolean = false

    private lateinit var dataView: CompletableFuture<ViewRenderable>
    private lateinit var mAddTextNodeDialog: BaseCustomDialog<DialogAddTextBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_bg)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)


        btn_add_image_dummy.setOnClickListener {
            askRequiredPermission()
        }

        btn_add_text_dummy.setOnClickListener {
            showAddTextDialog()
        }

        btn_clear.setOnClickListener {
            mPrefProvider.clear()
            showMessageDialog("All data saved in local device has been cleared.\nRestart the app.")
            mind_mapping_view.removeAllViewsInLayout()
        }
        initializeChooseOptionDialog()
        initializeSceneView()
        initializeQRScanner()

        //addParentNode("Bonjour")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG
            )
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    private fun initializeSceneView() {

        makeInfoView()
        arSceneView.scene.addOnUpdateListener(this::onUpdateFrame)

    }

    private fun initializeQRScanner() {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE,
                FirebaseVisionBarcode.FORMAT_AZTEC
            )
            .build()
    }

    private fun onUpdateFrame(frameTime: FrameTime) {
        try {
            frameTime.toString()
            val frame = arSceneView.arFrame
            frame?.let { myFrame ->

                val updatedAugmentedImages =
                    myFrame.getUpdatedTrackables(AugmentedImage::class.java)

                for (augmentedImage in updatedAugmentedImages) {
                    if (augmentedImage.trackingState == TrackingState.TRACKING) {
                        // Check camera image matches our reference image
                        when (augmentedImage.name) {
                            "Bonjour" -> {
                                //createRenderable(augmentedImage)
                                takePhoto(augmentedImage)
                            }
                            "Hallo" -> {
                                //createRenderable(augmentedImage)
                                takePhoto(augmentedImage)
                            }
                            "Ciao" -> {
                                //createRenderable(augmentedImage)
                                takePhoto(augmentedImage)
                            }
                            "Namaste" -> {
                                //createRenderable(augmentedImage)
                                takePhoto(augmentedImage)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.d("test", "test")
        }
    }

    private fun takeScreenshot(bitmap: Bitmap, augmentedImage: AugmentedImage) {

        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance()
            .visionBarcodeDetector

        val result = detector.detectInImage(image)
            .addOnSuccessListener { barcodes ->
                val names = sensorsMap.keys

                if (barcodes.size > 0) {
                    val sensorId = barcodes[0].displayValue!!
                    if (!names.contains(sensorId)) {
                        createRenderable(augmentedImage, sensorId)
                    }
                }
            }
            .addOnFailureListener {
                print(it.toString())
            }
    }

    private fun takePhoto(augmentedImage: AugmentedImage) {

        /*ArSceneView view = fragment.getArSceneView();*/
        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(
            arSceneView.width, arSceneView.height,
            Bitmap.Config.ARGB_8888
        )

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        try {
            PixelCopy.request(arSceneView, bitmap, { copyResult ->
                if (copyResult === PixelCopy.SUCCESS) {
                    takeScreenshot(bitmap, augmentedImage)
                    // println(bitmap)

                } else {
                    Log.d("DrawAR", "Failed to copyPixels: $copyResult")
                    /*val toast = Toast.makeText(
                        this@MainActivity,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG
                    )
                    toast.show()*/
                }
                handlerThread.quitSafely()
            }, Handler(handlerThread.looper))
        } catch (e: java.lang.Exception) {

        }
    }

    private fun setupAugmentedImageDb(config: Config): Boolean {
        val augmentedImageDatabase: AugmentedImageDatabase

        val augmentedImageBitmap = loadAugmentedImage() ?: return false

        augmentedImageDatabase = AugmentedImageDatabase(session)
        augmentedImageDatabase.addImage("Bonjour", augmentedImageBitmap)
        augmentedImageDatabase.addImage("Hallo", augmentedImageBitmap)
        augmentedImageDatabase.addImage("Ciao", augmentedImageBitmap)
        augmentedImageDatabase.addImage("Namaste", augmentedImageBitmap)


        config.augmentedImageDatabase = augmentedImageDatabase
        return true

    }

    private fun loadAugmentedImage(): Bitmap? {
        try {
            assets.open("Bonjour.jpg").use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
        }
        try {
            assets.open("Hallo.jpg").use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
        }
        try {
            assets.open("Ciao.jpg").use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
        }
        try {
            assets.open("Namaste.jpg").use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
        }

        return null
    }

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        if (!setupAugmentedImageDb(config)) {
            messageSnackBarHelper.showError(this, "Could not setup augmented image database")
        }
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        session!!.configure(config)
    }

    override fun onResume() {
        super.onResume()
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                    }
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this)
                    return
                }

                session = Session(/* context = */this)
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: Exception) {
                message = "This device does not support AR"
                exception = e
            }

            if (message != null) {
                messageSnackBarHelper.showError(this, message)
                Log.e(TAG, "Exception creating session", exception)
                return
            }

            shouldConfigureSession = true
        }

        if (shouldConfigureSession) {
            configureSession()
            shouldConfigureSession = false
            arSceneView.setupSession(session)
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
            arSceneView.resume()
        } catch (e: CameraNotAvailableException) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackBarHelper.showError(this, "Camera not available. Please restart the app.")
            session = null
            return
        }
    }

    private fun createRenderable(augmentedImage: AugmentedImage, name: String) {

        if (!mParentAdded) {
            mParentAdded = true
            addParentNode(name)
        }

        /* rl_mind_map.visibility = View.VISIBLE
         val item = Item(this@MainActivity, name, "Parent Node", true)
         mind_mapping_view.addCentralItem(item, false)
         mParentItem = item
         showButtons(item)*/

        /*try {
            var renderable: ViewRenderable? = null
            try {
                renderable = dataView.get()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
            val node = Node()
            try {

                val anchorNode =
                    AnchorNode(arSceneView.session.createAnchor(augmentedImage.centerPose))
                arSceneView.scene.removeChild(anchorNode)
                val pose = Pose.makeTranslation(0.0f, 0.0f, 0.12f)
                node.localPosition = Vector3(pose.tx(), pose.ty(), pose.tz())

                node.renderable = renderable
                node.setParent(anchorNode)
                node.localRotation = Quaternion(pose.qx(), 90f, -90f, pose.qw())

                arSceneView.scene.addChild(anchorNode)

                renderable?.let { myRenderable ->
                    myRenderable.view.language.text = name
                    sensorsMap[name] = myRenderable
                    mind_mapping_view.visibility = View.VISIBLE
                    addCenterItem(name)
                }
                makeInfoView()

            } catch (e: Exception) {
                e.toString()
            }
        } catch (ex: Exception) {
            showMessageDialog("Exception is ${ex.message}")
        }*/
    }

    private fun askRequiredPermission() {

        /**Asking permissions from user*/

        Dexter.withContext(this)
            .withPermissions(*Constants.CAMERA_AND_READ_STORAGE_PERMISSION)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        when {
                            report.areAllPermissionsGranted() -> {
                                mChooseOptionDialog?.show()
                            }
                            report.isAnyPermissionPermanentlyDenied -> {
                                showPermissionsAlert()
                            }
                            else -> {
                                showToast(getString(R.string.required_permissions_not_granted))
                            }
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .withErrorListener {
                showToast(it.name)
            }.check()
    }

    private fun initializeChooseOptionDialog() {
        mChooseOptionDialog = BaseCustomDialog(
            this,
            R.layout.dialog_choose_camera_gallery,
            object : BaseCustomDialog.DialogListener {
                override fun onViewClick(view: View?) {
                    view?.let { it ->

                        mChooseOptionDialog?.dismiss()

                        val cameraOptions = CropImageOptions()
                        cameraOptions.fixAspectRatio = true
                        cameraOptions.aspectRatioX = 1
                        cameraOptions.aspectRatioY = 1
                        cameraOptions.cropShape = CropImageView.CropShape.OVAL

                        val options = CropImageContractOptions(
                            null,
                            cameraOptions
                        )

                        when (it.id) {
                            R.id.rl_camera_pick -> {
                                cameraOptions.imageSourceIncludeCamera = true
                                cameraOptions.imageSourceIncludeGallery = false

                                //Launching the flow
                                cropImage.launch(options)
                            }
                            R.id.rl_gallery_pick -> {
                                cameraOptions.imageSourceIncludeCamera = false
                                cameraOptions.imageSourceIncludeGallery = true

                                //Launching the flow
                                cropImage.launch(options)
                            }
                        }
                    }
                }
            })

        Objects.requireNonNull<Window>(mChooseOptionDialog?.window).setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )
        mChooseOptionDialog?.setCancelable(true)
    }

    private fun showAddTextDialog() {
        mAddTextNodeDialog = BaseCustomDialog(
            this,
            R.layout.dialog_add_text,
            object : BaseCustomDialog.DialogListener {
                override fun onViewClick(view: View?) {
                    if (mParentItem != null) {
                        val text = mAddTextNodeDialog.getBinding().etMessage.text.toString()
                        if (text.isNotEmpty()) {
                            mAddTextNodeDialog.dismiss()
                            addChildNodeText(text)
                        } else {
                            showToast("Please enter some text.")
                        }
                    } else {
                        showToast("Parent node not found")
                    }
                }
            })

        mAddTextNodeDialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )
        mAddTextNodeDialog.setCancelable(true)
        mAddTextNodeDialog.show()
    }

    private fun saveNode(text: String) {

    }

    private val cropImage =
        (this as ComponentActivity).registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                val uriContent = result.uriContent
                uriContent?.let { mainContentUri ->
                    addChildNodeImage(mainContentUri)
                }
            }
        }

    /*private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            uriContent?.let { mainContentUri ->

                Glide.with(this)
                    .load(mainContentUri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(binding.imgProfile)

                val bitmap = getBitmapFromUri(mainContentUri)
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 40, bos)
                val data: ByteArray = bos.toByteArray()
                uploadFileApi(createMultiPart(data, "file"))
            }
        }
    }*/

    private fun makeInfoView() {
        dataView = ViewRenderable.builder().setView(this, R.layout.layout_bg).build()
    }

    private fun addParentNode(parentNodeText: String) {

        btn_add_text.setOnClickListener {
            showAddTextDialog()
        }

        btn_add_image.setOnClickListener {
            askRequiredPermission()
        }

        if (mPrefProvider.getNodesList().isNotEmpty()) {

            /*Setting parent node*/
            for (nodeModel in mPrefProvider.getNodesList()) {
                if (nodeModel.is_parent_node && nodeModel.parent_node_text == parentNodeText) {
                    rl_mind_map.visibility = View.VISIBLE
                    val item = Item(this@MainActivity, parentNodeText, "Parent", true)
                    mind_mapping_view.addCentralItem(item, false)
                    mParentItem = item
                }
            }

            if (mParentItem != null) {

                /*Setting all child nodes including images and texts*/

                val listOfChildNodes = mutableListOf<NodeModel>()

                for (nodeModel in mPrefProvider.getNodesList()) {
                    if (nodeModel.parent_node_text == parentNodeText && !nodeModel.is_parent_node) {
                        listOfChildNodes.add(nodeModel)
                    }
                }

                if (listOfChildNodes.isNotEmpty()) {
                    for (nodeModel in listOfChildNodes) {
                        if (nodeModel.is_text_field) {
                            val childNode =
                                Item(this@MainActivity, nodeModel.child_node_text, "Child", true)
                            mind_mapping_view.addItem(
                                childNode,
                                mParentItem,
                                40,
                                10,
                                ItemLocation.TOP,
                                true,
                                null
                            )
                        } else {
                            val uri = Uri.parse(nodeModel.child_node_image_uri)
                            uri?.let { mainContentUri ->
                                val imageItem = Item(this@MainActivity, mainContentUri, true)
                                mind_mapping_view.addItem(
                                    imageItem,
                                    mParentItem,
                                    40,
                                    10,
                                    ItemLocation.BOTTOM,
                                    true,
                                    null
                                )
                            }
                        }
                    }
                }
            } else {
                showToast("Parent node not found")
            }
        } else {

            val listOfNodes = mutableListOf<NodeModel>()
            val nodeModel = NodeModel()
            nodeModel.is_parent_node = true
            nodeModel.parent_node_text = parentNodeText
            listOfNodes.add(nodeModel)

            mPrefProvider.setNodesList(listOfNodes)

            rl_mind_map.visibility = View.VISIBLE
            val item = Item(this@MainActivity, parentNodeText, "Parent", true)
            mind_mapping_view.addCentralItem(item, false)
            mParentItem = item
        }
    }

    private fun addChildNodeText(childNodeText: String) {

        val listOfStoredNodes = mPrefProvider.getNodesList()
        val nodeModel = NodeModel()
        nodeModel.parent_node_text = mParentItem!!.title.text.toString()
        nodeModel.child_node_text = childNodeText
        nodeModel.is_text_field = true
        listOfStoredNodes.add(nodeModel)
        mPrefProvider.setNodesList(listOfStoredNodes)

        val childNode = Item(this@MainActivity, childNodeText, "Child", true)
        mind_mapping_view.addItem(
            childNode,
            mParentItem,
            40,
            10,
            ItemLocation.TOP,
            true,
            null
        )
    }

    private fun addChildNodeImage(mainContentUri: Uri) {

        //addDummyParent()

        val listOfStoredNodes = mPrefProvider.getNodesList()
        val nodeModel = NodeModel()
        nodeModel.parent_node_text = mParentItem!!.title.text.toString()
        nodeModel.child_node_image_uri = mainContentUri.toString()
        listOfStoredNodes.add(nodeModel)
        mPrefProvider.setNodesList(listOfStoredNodes)

        val imageItem = Item(this@MainActivity, mainContentUri, true)
        mind_mapping_view.addItem(
            imageItem,
            mParentItem,
            40,
            10,
            ItemLocation.BOTTOM,
            true,
            null
        )
    }

    private fun addDummyParent() {
        /**************************************************/

        /*Dummy parent image*/

        rl_mind_map.visibility = View.VISIBLE
        val itemParent = Item(this@MainActivity, "Bonjour", "Parent", true)
        mind_mapping_view.addCentralItem(itemParent, false)
        mParentItem = itemParent

        val listOfNodes = mPrefProvider.getNodesList()
        val nodeModelParent = NodeModel()
        nodeModelParent.is_parent_node = true
        nodeModelParent.parent_node_text = "Bonjour"
        listOfNodes.add(nodeModelParent)

        mPrefProvider.setNodesList(listOfNodes)

        /**************************************************/
    }
}


