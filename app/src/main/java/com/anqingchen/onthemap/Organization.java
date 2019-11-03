package com.anqingchen.onthemap;

import android.net.Uri;
import android.util.Log;

import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.auth.providers.userpassword.UserPasswordAuthProviderClient;
import com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordCredential;

import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;


public class Organization {

    public static final Codec<Organization> codec = new Codec<Organization>() {
        @Override
        public Organization decode(BsonReader reader, DecoderContext decoderContext) {
            final BsonDocument document = new BsonDocumentCodec().decode(reader, decoderContext);

            return fromBsonDocument(document);
        }

        @Override
        public void encode(BsonWriter writer, Organization value, EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<Organization> getEncoderClass() {
            return Organization.class;
        }
    };

    private ObjectId id;
    private String name, website, email;
    private char password[];

    // Constructor for authentication
    public Organization(String email, char password[]) {
        this.email = email;
        this.password = password;
    }

    // Constructor for retrieving organization info
    public Organization(ObjectId _id, String name, String website, String email) {
        this.id = _id;
        this.name = name;
        this.website = website;
        this.email = email;
    }

    // Constructor for registering new organizations
    public Organization(String name, String website, String email, char password[]) {
        this.name = name;
        this.website = website;
        this.email = email;
        this.password = password;
    }

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWebsite() {
        return website;
    }

    public String getEmail() {
        return email;
    }

    public char[] getPassword() {
        return password;
    }

    public void register() {
        UserPasswordAuthProviderClient emailPassClient = Stitch.getDefaultAppClient().getAuth().getProviderClient(
                UserPasswordAuthProviderClient.factory
        );

        emailPassClient.registerWithEmail(email, String.valueOf(password))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("stitch", "Successfully sent account confirmation email");
                    } else {
                        Log.e("stitch", "Error registering new organization:", task.getException());
                    }
                });
    }

    public static void resetPassword(String email) {
        UserPasswordAuthProviderClient emailPassClient = Stitch.getDefaultAppClient().getAuth().getProviderClient(
                UserPasswordAuthProviderClient.factory
        );

        emailPassClient.sendResetPasswordEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("stitch", "Successfully sent password reset email");
                    } else {
                        Log.e("stitch", "Error sending password reset email:", task.getException());
                    }
                });
    }

    public static void handlePasswordReset(Uri uri) {
        String token = uri.getQueryParameter("token");
        String tokenId = uri.getQueryParameter("tokenId");

        UserPasswordAuthProviderClient emailPassClient = Stitch
                .getDefaultAppClient()
                .getAuth()
                .getProviderClient(UserPasswordAuthProviderClient.factory);

        emailPassClient.confirmUser(token, tokenId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("stitch", "Successfully reset organization's password");
                    } else {
                        Log.e("stitch", "Error resetting organization's password:", task.getException());
                    }
                });
    }

    private static Organization fromBsonDocument(BsonDocument document) {
        return new Organization(document.getObjectId("_id").getValue(),
                document.getString("name").getValue(), document.getString("website").getValue(),
                document.getString("email").getValue());
    }

    private static BsonDocument toBsonDocument(Organization organization) {
        BsonDocument temp = new BsonDocument();

        temp.put("_id", new BsonObjectId());
        temp.put("name", new BsonString(organization.name));
        temp.put("website", new BsonString(organization.website));

        return temp;
    }
}
