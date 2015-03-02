package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;


public class SwitchesFragment extends Fragment {

    public static final String ARG_OBJECT = "object";


    public SwitchesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_switches, container, false);

        Bundle args = getArguments();
        /*((TextView) rootView.findViewById(android.R.id.text1)).setText(
                Integer.toString(args.getInt(ARG_OBJECT)));*/

        return rootView;

    }
}
