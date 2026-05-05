package com.sergio.flatshare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.SessionStore;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        TextView emailTv = view.findViewById(R.id.profileEmailTv);
        TextView uidTv = view.findViewById(R.id.profileUidTv);
        TextView currentGroupTv = view.findViewById(R.id.profileCurrentGroupTv);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        emailTv.setText(user != null && user.getEmail() != null ? user.getEmail() : "Sin email");
        uidTv.setText(user != null ? user.getUid() : "Sin UID");

        String currentGroup = SessionStore.getCurrentGroup(requireContext());
        currentGroupTv.setText(currentGroup == null ? "Ninguno seleccionado" : currentGroup);

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }
}
