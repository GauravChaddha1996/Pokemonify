package com.pokify.pokify;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.pokify.pokify.UIComponents.CommonAdapter;
import com.pokify.pokify.fragments.MainFragment;
import com.pokify.pokify.fragments.MyCardDetailFragment;
import com.pokify.pokify.fragments.MyCardListFragment;
import com.pokify.pokify.fragments.PokemonDetailFragment;
import com.pokify.pokify.fragments.PokemonEditFragment;
import com.pokify.pokify.fragments.PokemonListFragment;
import com.pokify.pokify.pokemondatabase.PokemonDto;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CommonAdapter.OnGetViewListener<String> {
    public static final int CAM_CODE = 234;
    public static final int READ_WRITE = 235;
    public static final int READ_WRITE_2 = 236;
    Toolbar toolbar;
    MaterialSearchView searchView;
    Fragment currentFragment;
    boolean submitFlag = false;
    AppBarLayout mAppBarLayout;
    Uri cameraUri;
    ContentValues values;
    String searchString = "-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermissions();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setItemTextAppearance(R.style.textStyleArchRival);
        navigationView.setItemTextColor(ColorStateList.valueOf(Color.WHITE));
        navigationView.setNavigationItemSelectedListener(this);
        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setSuggestions(getResources().getStringArray(R.array.pokemon_name));
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                submitFlag = true;
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (submitFlag) {
                    submitFlag = !submitFlag;
                } else {
                    Log.d("abad", "onTextQueryChanged");
                    search(newText);
                }
                return true;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                if (currentFragment instanceof MyCardListFragment) {
                    searchView.setSuggestions(new String[]{});
                    supportInvalidateOptionsMenu();
                } else if (!(currentFragment instanceof PokemonListFragment)) {
                    searchView.setSuggestions(getResources().getStringArray(R.array.pokemon_name));
                    changeFrag(new PokemonListFragment());
                } else {
                    searchView.setSuggestions(getResources().getStringArray(R.array.pokemon_name));
                    supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onSearchViewClosed() {
            }
        });
        currentFragment = new MainFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, currentFragment);
        fragmentTransaction.commitAllowingStateLoss();
        supportInvalidateOptionsMenu();
    }

    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_WRITE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    public void search(String query) {
        if (currentFragment instanceof PokemonListFragment) {
            searchString = query;
            PokemonListFragment pokemonListFragment = (PokemonListFragment) currentFragment;
            pokemonListFragment.search(query);
        } else if (currentFragment instanceof MyCardListFragment) {
            searchString = query;
            ((MyCardListFragment) currentFragment).search(searchString);
        }
    }

    public void hideSearch() {
        searchView.closeSearch();
    }

    public void changeFrag(Fragment fragment) {
        if (currentFragment instanceof PokemonEditFragment) {
            checkAndSaveCard(fragment);
        } else if (currentFragment instanceof PokemonDetailFragment) {
            PokemonListFragment pokemonListFragment = new PokemonListFragment();
            pokemonListFragment.setPreScrollName(((PokemonDetailFragment) currentFragment).getPokemonDto().getName());
            doFragTransaction(pokemonListFragment, 0);
        } else if (currentFragment instanceof MyCardDetailFragment) {
            MyCardDetailFragment myCardDetailFragment = (MyCardDetailFragment) currentFragment;
            if (myCardDetailFragment.getEditing()) {
                ((MyCardDetailFragment) currentFragment).saveAndToggleAndChange(fragment);
            } else {
                doFragTransaction(fragment, 0);
            }
        } else {
            doFragTransaction(fragment, 0);
        }
    }

    public boolean doFragTransaction(Fragment fragment, int anim) {
        currentFragment = fragment;
        if (currentFragment instanceof MainFragment) {
            mAppBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
            mAppBarLayout.setExpanded(true);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (anim == 0) {
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        fragmentTransaction.commitAllowingStateLoss();
        supportInvalidateOptionsMenu();
        return true;
    }

    private void checkAndSaveCard(final Fragment fragment) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to save the card?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((PokemonEditFragment) currentFragment).saveMyCard(fragment);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                doFragTransaction(new MainFragment(), 0);
            }
        });
        builder.show();
    }

    public void startPokeImagePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select source");
        final CommonAdapter<String> commonAdapter = new CommonAdapter<>(this);
        List<String> list = new ArrayList<>();
        list.add("Camera");
        list.add("Gallery");
        commonAdapter.setList(list);
        builder.setAdapter(commonAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if ("Camera".equals(commonAdapter.getItem(i))) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_CONTACTS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, CAM_CODE);
                    } else {
                        startCamera();
                    }
                } else {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), 22);
                }
            }
        });
        builder.show();
    }

    public void startCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        cameraUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        startActivityForResult(intent, 23);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAM_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                }
                break;
            }
            case READ_WRITE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_WRITE_2);
                }
                break;
            }
            case READ_WRITE_2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Handler handler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            System.exit(1);
                        }
                    };
                    Toast.makeText(this, "External Storage permission used for saving images denied." +
                            "Please allow this for the app to function properly.Byee..", Toast.LENGTH_LONG).show();
                    handler.sendEmptyMessageDelayed(0, 3000);
                }
                break;
            }
        }
    }

    public void shareImage(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tmp" + System.currentTimeMillis(), ".jpg", this.getExternalCacheDir());
            // write the bytes in file
            FileOutputStream fo = new FileOutputStream(tempFile);
            fo.write(bytes.toByteArray());
            fo.close();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/jpeg");
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(tempFile.getAbsolutePath()));
            startActivity(Intent.createChooser(share, "Share Pokemon"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 22 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();
            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(4, 3)
                    .setFixAspectRatio(true)
                    .setOutputCompressQuality(30)
                    .start(this);
        }
        if (requestCode == 23 && resultCode == RESULT_OK) {
            CropImage.activity(cameraUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(4, 3)
                    .setFixAspectRatio(true)
                    .setOutputCompressQuality(30)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result.getUri());
                    bitmap = Bitmap.createScaledBitmap(bitmap, 900, 700, false);
                    if (currentFragment instanceof PokemonEditFragment) {
                        ((PokemonEditFragment) currentFragment).setPokemonImage(Utils.getRoundedCornerBitmap(bitmap));
                    } else {
                        ((MyCardDetailFragment) currentFragment).setPokemonImage(Utils.getRoundedCornerBitmap(bitmap));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    public View getView(View convertView, String item, int position) {
        MyDialogViewHolder myDialogViewHolder;
        if (convertView == null) {
            myDialogViewHolder = new MyDialogViewHolder();
            convertView = LayoutInflater.from(this).inflate(R.layout.activity_imagepicker, null);
            myDialogViewHolder.mTextView = (TextView) convertView.findViewById(R.id.dialogListText);
            myDialogViewHolder.mImageView = (ImageView) convertView.findViewById(R.id.dialogListImage);
            convertView.setTag(myDialogViewHolder);
        } else {
            myDialogViewHolder = (MyDialogViewHolder) convertView.getTag();
        }
        if (item.equals("Camera")) {
            myDialogViewHolder.mTextView.setText("Camera");
            myDialogViewHolder.mImageView.setImageDrawable(getResources().getDrawable(R.drawable.camera));
        } else {
            myDialogViewHolder.mTextView.setText("Gallery");
            myDialogViewHolder.mImageView.setImageDrawable(getResources().getDrawable(R.drawable.gallery));
        }
        return convertView;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            if (currentFragment instanceof MyCardDetailFragment) {
                changeFrag(new MyCardListFragment());
            } else if (!(currentFragment instanceof MainFragment)) {
                changeFrag(new MainFragment());
            } else {
                super.onBackPressed();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (currentFragment instanceof MainFragment) {
            getMenuInflater().inflate(R.menu.main, menu);
            MenuItem item = menu.findItem(R.id.action_search);
            searchView.setMenuItem(item);
        } else if (currentFragment instanceof PokemonDetailFragment) {
            getMenuInflater().inflate(R.menu.pokemondetail, menu);
        } else if (currentFragment instanceof PokemonEditFragment) {
            getMenuInflater().inflate(R.menu.pokemonedit, menu);
        } else if (currentFragment instanceof MyCardDetailFragment) {
            getMenuInflater().inflate(R.menu.mycardetial, menu);
            MenuItem item = menu.findItem(R.id.action_mycard_edit);
            if (((MyCardDetailFragment) currentFragment).getMaterialDialogCreator() != null) {
                if (((MyCardDetailFragment) currentFragment).getEditing()) {
                    item.setIcon(R.drawable.save);
                } else {
                    item.setIcon(R.drawable.edit);
                }
            }
        } else if (currentFragment instanceof MyCardListFragment) {
            getMenuInflater().inflate(R.menu.mycardlist, menu);
            MenuItem item = menu.findItem(R.id.action_my_card_list_search);
            searchView.setMenuItem(item);
        } else {
            getMenuInflater().inflate(R.menu.other, menu);
            MenuItem item = menu.findItem(R.id.action_search);
            searchView.setMenuItem(item);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_share_myPokemon) {
            MainFragment mainFragment = (MainFragment) currentFragment;
            mainFragment.shareMyPokemon();
        } else if (id == R.id.action_save_myPokemon) {
            MainFragment mainFragment = (MainFragment) currentFragment;
            mainFragment.saveMyPokemon();
        } else if (id == R.id.action_share) {
            PokemonDetailFragment pokemonDetailFragment = (PokemonDetailFragment) currentFragment;
            pokemonDetailFragment.shareThisPokemon();
        } else if (id == R.id.action_save) {
            PokemonDetailFragment pokemonDetailFragment = (PokemonDetailFragment) currentFragment;
            pokemonDetailFragment.saveMyPokemon();
        } else if (id == R.id.action_edit) {
            PokemonEditFragment pokemonEditFragment = new PokemonEditFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable("PokemonDto", ((PokemonDetailFragment) currentFragment).getPokemonDto());
            pokemonEditFragment.setArguments(bundle);
            doFragTransaction(pokemonEditFragment, 1);
        } else if (id == R.id.action_mycard_share) {
            MyCardDetailFragment myCardDetailFragment = (MyCardDetailFragment) currentFragment;
            myCardDetailFragment.shareThisPokemon();
        } else if (id == R.id.action_mycard_edit) {
            MyCardDetailFragment myCardDetailFragment = (MyCardDetailFragment) currentFragment;
            if (myCardDetailFragment.getEditing()) {
                myCardDetailFragment.saveAndToggle();
            } else {
                myCardDetailFragment.toggleShouldEdit();
                item.setIcon(R.drawable.save);
            }
        } else if (id == R.id.action_mycard_delete) {
            MyCardDetailFragment myCardDetailFragment = (MyCardDetailFragment) currentFragment;
            myCardDetailFragment.deleteCard();
        } else if (id == R.id.action_mycard_gallery) {
            MyCardDetailFragment myCardDetailFragment = (MyCardDetailFragment) currentFragment;
            myCardDetailFragment.saveInGallery();
        } else if (id == R.id.action_editfrag_save) {
            PokemonEditFragment pokemonEditFragment = (PokemonEditFragment) currentFragment;
            pokemonEditFragment.saveMyCard(new MyCardListFragment());
        } else if (id == R.id.action_editfrag_share) {
            PokemonEditFragment pokemonEditFragment = (PokemonEditFragment) currentFragment;
            pokemonEditFragment.shareThisPokemon();
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            if (!(currentFragment instanceof MainFragment)) {
                changeFrag(new MainFragment());
            }
        } else if (id == R.id.nav_poke_list) {
            if (!(currentFragment instanceof PokemonListFragment)) {
                changeFrag(new PokemonListFragment());
            }
        } else if (id == R.id.nav_my_cards) {
            if (!(currentFragment instanceof MyCardListFragment)) {
                changeFrag(new MyCardListFragment());
            }
        } else if (id == R.id.nav_create_card) {
            if (!(currentFragment instanceof PokemonEditFragment)) {
                PokemonEditFragment pokemonEditFragment = new PokemonEditFragment();
                PokemonDto pokemonDto = new PokemonDto(200, "pokeball", 25, "pokeball", "Pokemon Type", "Click " +
                        "items to edit", "", 60, 4, 120);
                Bundle bundle = new Bundle();
                bundle.putSerializable("PokemonDto", pokemonDto);
                pokemonEditFragment.setArguments(bundle);
                changeFrag(pokemonEditFragment);
            }
        } else if (id == R.id.nav_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.aboutdialogview, null);
            builder.setView(view);
            AlertDialog a = builder.create();
            a.show();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class MyDialogViewHolder {
        TextView mTextView;
        ImageView mImageView;
    }
}
