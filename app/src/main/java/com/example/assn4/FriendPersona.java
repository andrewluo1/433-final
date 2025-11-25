package com.example.assn4;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class FriendPersona {

    public String name;
    public int avatarRes;
    public String style;

    public FriendPersona(String name, int avatarRes, String style) {
        this.name = name;
        this.avatarRes = avatarRes;
        this.style = style;
    }

    public static List<FriendPersona> defaultFriends(Context ctx) {
        List<FriendPersona> f = new ArrayList<>();

        f.add(new FriendPersona(
                "Ericka",
                R.drawable.f1,
                "supportive, encouraging, gentle, positive, warm tone"
        ));

        f.add(new FriendPersona(
                "Kanye",
                R.drawable.f2,
                "sarcastic, dry humor, witty, slightly teasing"
        ));

        f.add(new FriendPersona(
                "Obama",
                R.drawable.f3,
                "mean"
        ));

        f.add(new FriendPersona(
                "Dwayne",
                R.drawable.f4,
                "poetic, emotional, expressive, aesthetic language"
        ));

        f.add(new FriendPersona(
                "Kevin",
                R.drawable.f5,
                "logical, analytical, practical, focused on details"
        ));

        return f;
    }
}
