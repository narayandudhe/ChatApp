package com.narayandudhe.chatApp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.narayandudhe.chatApp.data.FriendDB;
import com.narayandudhe.chatApp.data.GroupDB;
import com.narayandudhe.chatApp.data.StaticConfig;
import com.narayandudhe.chatApp.model.Group;
import com.narayandudhe.chatApp.model.ListFriend;
import com.narayandudhe.chatApp.model.Room;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;



public class AddGroupActivity extends AppCompatActivity {

    private RecyclerView recyclerListFriend;
    private ListPeopleAdapter adapter;
    private ListFriend listFriend;
    private LinearLayout btnAddGroup;
    private Set<String> listIDChoose;
    private Set<String> listIDRemove;
    private EditText editTextGroupName;
    private TextView txtGroupIcon, txtActionName;
    private LovelyProgressDialog dialogWait;
    private boolean isEditGroup;
    private Group groupEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.narayandudhe.chatApp.R.layout.activity_add_group);

        Intent intentData = getIntent();
        txtActionName = (TextView) findViewById(com.narayandudhe.chatApp.R.id.txtActionName);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        listFriend = FriendDB.getInstance(this).getListFriend();
        listIDChoose = new HashSet<>();
        listIDRemove = new HashSet<>();
        listIDChoose.add(StaticConfig.UID);
        btnAddGroup = (LinearLayout) findViewById(com.narayandudhe.chatApp.R.id.btnAddGroup);
        editTextGroupName = (EditText) findViewById(com.narayandudhe.chatApp.R.id.editGroupName);
        txtGroupIcon = (TextView) findViewById(com.narayandudhe.chatApp.R.id.icon_group);
        dialogWait = new LovelyProgressDialog(this).setCancelable(false);
        editTextGroupName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() >= 1) {
                    txtGroupIcon.setText((charSequence.charAt(0) + "").toUpperCase());
                } else {
                    txtGroupIcon.setText("R");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnAddGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listIDChoose.size() < 3) {
                    Toast.makeText(AddGroupActivity.this, "Add at lease two people to create group", Toast.LENGTH_SHORT).show();
                } else {
                    if (editTextGroupName.getText().length() == 0) {
                        Toast.makeText(AddGroupActivity.this, "Enter group name", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isEditGroup) {
                            editGroup();
                        } else {
                            createGroup();
                        }
                    }
                }
            }
        });

        if (intentData.getStringExtra("groupId") != null) {
            isEditGroup = true;
            String idGroup = intentData.getStringExtra("groupId");
            txtActionName.setText("Save");
            btnAddGroup.setBackgroundColor(getResources().getColor(com.narayandudhe.chatApp.R.color.colorPrimary));
            groupEdit = GroupDB.getInstance(this).getGroup(idGroup);
            editTextGroupName.setText(groupEdit.groupInfo.get("name"));
        } else {
            isEditGroup = false;
        }

        recyclerListFriend = (RecyclerView) findViewById(com.narayandudhe.chatApp.R.id.recycleListFriend);
        recyclerListFriend.setLayoutManager(linearLayoutManager);
        adapter = new ListPeopleAdapter(this, listFriend, btnAddGroup, listIDChoose, listIDRemove, isEditGroup, groupEdit);
        recyclerListFriend.setAdapter(adapter);


    }

    private void editGroup() {
        //Show dialog wait
        dialogWait.setIcon(com.narayandudhe.chatApp.R.drawable.ic_add_group_dialog)
                .setTitle("Editing....")
                .setTopColorRes(com.narayandudhe.chatApp.R.color.colorPrimary)
                .show();
        //Delete group
        final String idGroup = groupEdit.id;
        Room room = new Room();
        for (String id : listIDChoose) {
            room.member.add(id);
        }
        room.groupInfo.put("name", editTextGroupName.getText().toString());
        room.groupInfo.put("admin", StaticConfig.UID);
        FirebaseDatabase.getInstance().getReference().child("group/" + idGroup).setValue(room)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        addRoomForUser(idGroup, 0);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialogWait.dismiss();
                        new LovelyInfoDialog(AddGroupActivity.this) {
                            @Override
                            public LovelyInfoDialog setConfirmButtonText(String text) {
                                findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dismiss();
                                    }
                                });
                                return super.setConfirmButtonText(text);
                            }
                        }
                                .setTopColorRes(com.narayandudhe.chatApp.R.color.colorAccent)
                                .setIcon(com.narayandudhe.chatApp.R.drawable.ic_add_group_dialog)
                                .setTitle("False")
                                .setMessage("Cannot connect database")
                                .setCancelable(false)
                                .setConfirmButtonText("Ok")
                                .show();
                    }
                })
        ;
    }

    private void createGroup() {
        //Show dialog wait
        dialogWait.setIcon(com.narayandudhe.chatApp.R.drawable.ic_add_group_dialog)
                .setTitle("Registering....")
                .setTopColorRes(com.narayandudhe.chatApp.R.color.colorPrimary)
                .show();

        final String idGroup = (StaticConfig.UID + System.currentTimeMillis()).hashCode() + "";
        Room room = new Room();
        for (String id : listIDChoose) {
            room.member.add(id);
        }
        room.groupInfo.put("name", editTextGroupName.getText().toString());
        room.groupInfo.put("admin", StaticConfig.UID);
        FirebaseDatabase.getInstance().getReference().child("group/" + idGroup).setValue(room).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                addRoomForUser(idGroup, 0);
            }
        });
    }

    private void deleteRoomForUser(final String roomId, final int userIndex) {
        if (userIndex == listIDRemove.size()) {
            dialogWait.dismiss();
            Toast.makeText(this, "Edit group success", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, null);
            AddGroupActivity.this.finish();
        } else {
            FirebaseDatabase.getInstance().getReference().child("user/" + listIDRemove.toArray()[userIndex] + "/group/" + roomId).removeValue()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            deleteRoomForUser(roomId, userIndex + 1);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dialogWait.dismiss();
                            new LovelyInfoDialog(AddGroupActivity.this) {
                                @Override
                                public LovelyInfoDialog setConfirmButtonText(String text) {
                                    findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dismiss();
                                        }
                                    });
                                    return super.setConfirmButtonText(text);
                                }
                            }
                                    .setTopColorRes(com.narayandudhe.chatApp.R.color.colorAccent)
                                    .setIcon(com.narayandudhe.chatApp.R.drawable.ic_add_group_dialog)
                                    .setTitle("False")
                                    .setMessage("Cannot connect database")
                                    .setCancelable(false)
                                    .setConfirmButtonText("Ok")
                                    .show();
                        }
                    });
        }
    }

    private void addRoomForUser(final String roomId, final int userIndex) {
        if (userIndex == listIDChoose.size()) {
            if (!isEditGroup) {
                dialogWait.dismiss();
                Toast.makeText(this, "Create group success", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, null);
                AddGroupActivity.this.finish();
            } else {
                deleteRoomForUser(roomId, 0);
            }
        } else {
            FirebaseDatabase.getInstance().getReference().child("user/" + listIDChoose.toArray()[userIndex] + "/group/" + roomId).setValue(roomId).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    addRoomForUser(roomId, userIndex + 1);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    dialogWait.dismiss();
                    new LovelyInfoDialog(AddGroupActivity.this) {
                        @Override
                        public LovelyInfoDialog setConfirmButtonText(String text) {
                            findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dismiss();
                                }
                            });
                            return super.setConfirmButtonText(text);
                        }
                    }
                            .setTopColorRes(com.narayandudhe.chatApp.R.color.colorAccent)
                            .setIcon(com.narayandudhe.chatApp.R.drawable.ic_add_group_dialog)
                            .setTitle("False")
                            .setMessage("Create group false")
                            .setCancelable(false)
                            .setConfirmButtonText("Ok")
                            .show();
                }
            });
        }
    }
}

class ListPeopleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ListFriend listFriend;
    private LinearLayout btnAddGroup;
    private Set<String> listIDChoose;
    private Set<String> listIDRemove;
    private boolean isEdit;
    private Group editGroup;

    public ListPeopleAdapter(Context context, ListFriend listFriend, LinearLayout btnAddGroup, Set<String> listIDChoose, Set<String> listIDRemove, boolean isEdit, Group editGroup) {
        this.context = context;
        this.listFriend = listFriend;
        this.btnAddGroup = btnAddGroup;
        this.listIDChoose = listIDChoose;
        this.listIDRemove = listIDRemove;

        this.isEdit = isEdit;
        this.editGroup = editGroup;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(com.narayandudhe.chatApp.R.layout.rc_item_add_friend, parent, false);
        return new ItemFriendHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ItemFriendHolder) holder).txtName.setText(listFriend.getListFriend().get(position).name);
        ((ItemFriendHolder) holder).txtEmail.setText(listFriend.getListFriend().get(position).email);
        String avata = listFriend.getListFriend().get(position).avata;
        final String id = listFriend.getListFriend().get(position).id;
        if (!avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(avata, Base64.DEFAULT);
            ((ItemFriendHolder) holder).avata.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
        }else{
            ((ItemFriendHolder) holder).avata.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), com.narayandudhe.chatApp.R.drawable.default_avata));
        }
        ((ItemFriendHolder) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    listIDChoose.add(id);
                    listIDRemove.remove(id);
                } else {
                    listIDRemove.add(id);
                    listIDChoose.remove(id);
                }
                if (listIDChoose.size() >= 3) {
                    btnAddGroup.setBackgroundColor(context.getResources().getColor(com.narayandudhe.chatApp.R.color.colorPrimary));
                } else {
                    btnAddGroup.setBackgroundColor(context.getResources().getColor(com.narayandudhe.chatApp.R.color.grey_500));
                }
            }
        });
        if (isEdit && editGroup.member.contains(id)) {
            ((ItemFriendHolder) holder).checkBox.setChecked(true);
        }else if(editGroup != null && !editGroup.member.contains(id)){
            ((ItemFriendHolder) holder).checkBox.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return listFriend.getListFriend().size();
    }
}

class ItemFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtName, txtEmail;
    public CircleImageView avata;
    public CheckBox checkBox;

    public ItemFriendHolder(View itemView) {
        super(itemView);
        txtName = (TextView) itemView.findViewById(com.narayandudhe.chatApp.R.id.txtName);
        txtEmail = (TextView) itemView.findViewById(com.narayandudhe.chatApp.R.id.txtEmail);
        avata = (CircleImageView) itemView.findViewById(com.narayandudhe.chatApp.R.id.icon_avata);
        checkBox = (CheckBox) itemView.findViewById(com.narayandudhe.chatApp.R.id.checkAddPeople);
    }
}

