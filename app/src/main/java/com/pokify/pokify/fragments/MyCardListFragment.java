package com.pokify.pokify.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pokify.pokify.MainActivity;
import com.pokify.pokify.R;
import com.pokify.pokify.UIComponents.CommonAdapter;
import com.pokify.pokify.Utils;
import com.pokify.pokify.pokemondatabase.DbHelper;
import com.pokify.pokify.pokemondatabase.PokemonDto;
import com.pokify.pokify.recyclerviewcomponents.ItemClickSupport;
import com.pokify.pokify.recyclerviewcomponents.PokemonListAdapter;
import com.pokify.pokify.recyclerviewcomponents.RecyclerViewEmptyExtdener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MyCardListFragment extends Fragment implements CommonAdapter.OnGetViewListener<String> {
    PokemonListAdapter mPokemonListAdapter;
    RecyclerViewEmptyExtdener mRecyclerView;
    TextView mEmptyView;
    List<PokemonDto> nameList;
    ProgressDialog progressDialog;
    int temp = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("My Cards");
        nameList = DbHelper.getInstance().getAllMyCards();
        Collections.reverse(nameList);
        View view = inflater.inflate(R.layout.fragment_pokemon_list, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View v) {
        mRecyclerView = (RecyclerViewEmptyExtdener) v.findViewById(R.id.pokemonRecyclerView);
        mEmptyView = (TextView) v.findViewById(R.id.emptyRecyclerView);
        mEmptyView.setText("No Cards :(");
        mRecyclerView.setEmptyView(mEmptyView);
        if (nameList.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
        mPokemonListAdapter = new PokemonListAdapter(getActivity(), nameList);
        mRecyclerView.setAdapter(mPokemonListAdapter);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        ItemClickSupport.addTo(mRecyclerView).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Select action");
                final CommonAdapter<String> commonAdapter = new CommonAdapter<>(MyCardListFragment.this);
                final List<String> list = new ArrayList<>();
                list.add("Delete this card");
                commonAdapter.setList(list);
                builder.setAdapter(commonAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (list.get(i).equals("Delete this card")) {
                            AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
                            builder.setTitle("Are you sure??");
                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    progressDialog = new ProgressDialog(getActivity());
                                    progressDialog.setTitle("Deleting the card");
                                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                    executorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialogInterface) {
                                                    dismissListener();
                                                }
                                            });
                                            temp = DbHelper.getInstance().deleteCard(mPokemonListAdapter.
                                                    getPokeList().get(position).getId());
                                            Log.d("TAG", "temp" + temp + "");
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    nameList=DbHelper.getInstance().getAllMyCards();
                                                    mPokemonListAdapter.setPokeList(nameList);
                                                    mPokemonListAdapter.notifyDataSetChanged();
                                                }
                                            });
                                            progressDialog.dismiss();
                                        }
                                    });
                                    executorService.shutdown();
                                }
                            });
                            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            builder.show();
                        }
                    }
                });
                builder.show();
                return true;
            }
        });
        ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                PokemonDto clickedPokemon = mPokemonListAdapter.getPokeList().get(position);
                MainActivity mainActivity = (MainActivity) getActivity();
                Bundle bundle = new Bundle();
                bundle.putSerializable("PokemonDto", clickedPokemon);
                MyCardDetailFragment detailFragment = new MyCardDetailFragment();
                detailFragment.setArguments(bundle);
                Utils.hideKeyboard(mainActivity);
                mainActivity.hideSearch();
                mainActivity.changeFrag(detailFragment);
            }
        });
    }

    private void dismissListener() {
        if (temp == 0) {
            Toast.makeText(getActivity(), "Oops we couldn't delete the pokemon.", Toast.LENGTH_SHORT).show();
        } else {
            mPokemonListAdapter.notifyDataSetChanged();
        }
    }

    public void search(String query) {
        Set<PokemonDto> newSet = new HashSet<>();
        for (PokemonDto p : nameList) {
            if (p.getName().toLowerCase().contains(query.toLowerCase())) {
                newSet.add(p);
            }
        }
        mPokemonListAdapter.setPokeList(new ArrayList<PokemonDto>(newSet));
        mPokemonListAdapter.notifyDataSetChanged();
    }

    @Override
    public View getView(View convertView, String item, int position) {
        MyDialogViewHolder myDialogViewHolder;
        if (convertView == null) {
            myDialogViewHolder = new MyDialogViewHolder();
            convertView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_imagepicker, null);
            myDialogViewHolder.mTextView = (TextView) convertView.findViewById(R.id.dialogListText);
            myDialogViewHolder.mImageView = (ImageView) convertView.findViewById(R.id.dialogListImage);
            convertView.setTag(myDialogViewHolder);
        } else {
            myDialogViewHolder = (MyDialogViewHolder) convertView.getTag();
        }
        myDialogViewHolder.mTextView.setText("Delete this card");
        myDialogViewHolder.mImageView.setImageDrawable(getResources().getDrawable(R.drawable.delete));
        return convertView;
    }

    private class MyDialogViewHolder {
        TextView mTextView;
        ImageView mImageView;
    }
}
