package com.slygames.facade.services.shizuku;

// Bound via Shizuku.bindUserService into a separate process running with the Shizuku daemon's
// (shell/ADB) privileges. AIDL only supports a single primitive/Parcelable return value per
// call, so exec() returns its exit code/stdout/stderr packed into one String - see
// ShizukuExecCodec for the encode/decode side of that.
interface IUserService {
    String exec(String command);
}
