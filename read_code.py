import os

# 1. Cấu hình: Bạn có thể thêm bao nhiêu đường dẫn tùy thích vào list này
root_dirs = [
    r"C:\Users\HNIRT\AndroidStudioProjects\MinLish2test5\app\src\main\java\com\example\minlish",
    r"C:\Users\HNIRT\AndroidStudioProjects\MinLish2test5\app\src\main\res",

]

extensions = (".kt", ".java", ".xml", ".gradle", ".toml")
output_file = "result.txt"

# 2. Các thư mục muốn loại bỏ
exclude_dirs = {
    "build", ".gradle", ".idea", "target", "bin", "obj",
    "drawable", "mipmap", "values-night", "raw", "debug"
}

print(f"--- Đang bắt đầu quét danh sách thư mục ---")

with open(output_file, "w", encoding="utf-8") as out:
    out.write("FULL PROJECT CONTEXT FOR AI ASSISTANT\n")
    out.write(f"DIRECTORIES SCANNED: {', '.join(root_dirs)}\n")
    out.write("="*60 + "\n\n")

    file_count = 0

    # Lặp qua từng thư mục trong list
    for current_root_dir in root_dirs:
        if not os.path.exists(current_root_dir):
            print(f"⚠️ Cảnh báo: Thư mục không tồn tại: {current_root_dir}")
            continue

        print(f"\n>> Đang quét: {current_root_dir}")

        for root, dirs, files in os.walk(current_root_dir):
            # Loại bỏ thư mục rác
            dirs[:] = [d for d in dirs if d not in exclude_dirs]

            for file in files:
                if file.endswith(extensions):
                    file_path = os.path.join(root, file)
                    file_count += 1

                    out.write(f"--- START OF FILE: {file} ---\n")
                    out.write(f"PATH: {file_path}\n")
                    out.write("CONTENT:\n\n")

                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            out.write(f.read())
                    except Exception as e:
                        out.write(f"[LỖI KHÔNG ĐỌC ĐƯỢC FILE: {str(e)}]")

                    out.write(f"\n\n--- END OF FILE: {file} ---\n")
                    out.write("="*60 + "\n\n")
                    # In ngắn gọn để không làm rối console
                    print(f"  + {file}")

print(f"\n--- XONG! ---")
print(f"Tổng số file đã vét: {file_count}")
print(f"Kết quả lưu tại: {os.path.abspath(output_file)}")