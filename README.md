# Study Planner - Android Java XML SQLite

Ứng dụng quản lý kế hoạch học tập cá nhân dành cho sinh viên trên Android.

## Công nghệ

- Java 17
- XML Layout và Material Components
- SQLiteOpenHelper
- SharedPreferences
- RecyclerView
- BottomNavigationView
- AlarmManager và Notification
- ExecutorService cho truy vấn dữ liệu nền

## Cấu trúc source

```text
com.example.personalplanner
├── activity/       Activity đăng nhập, kế hoạch và môn học
├── adapter/        RecyclerView adapter
├── data/
│   ├── local/      SQLite DatabaseHelper
│   └── model/      User, Course, StudyPlan, StudyStatistics
├── fragment/       Tổng quan, kế hoạch, lịch và hồ sơ
├── notification/   Lập lịch và hiển thị nhắc học
└── utils/          Session và mã hóa mật khẩu
```

## Thực thể dữ liệu

- `users`: tài khoản người dùng.
- `courses`: môn học, mã môn, giảng viên và màu nhận diện.
- `tasks`: kế hoạch học tập, môn học, ngày giờ, mức ưu tiên, thời lượng, tiến độ và nhắc lịch.

Quan hệ:

- Một người dùng có nhiều môn học.
- Một người dùng có nhiều kế hoạch học tập.
- Một môn học có nhiều kế hoạch học tập.

## Chức năng

- Đăng ký, đăng nhập và lưu phiên.
- CRUD môn học.
- CRUD kế hoạch học tập.
- Phân loại kế hoạch theo môn học.
- Tìm kiếm và lọc theo trạng thái, môn học.
- Chọn mức ưu tiên và thời lượng dự kiến.
- Đánh dấu hoàn thành.
- Xem kế hoạch theo lịch.
- Nhắc lịch học bằng notification.
- Thống kê số môn, số kế hoạch, tiến độ và tổng giờ dự kiến.
- Dark mode theo hệ thống.

## Chạy project

1. Mở thư mục project bằng Android Studio.
2. Chọn JDK 17 cho Gradle.
3. Sync Gradle.
4. Chạy trên thiết bị hoặc emulator API 23 trở lên.
5. Trên Android 13 trở lên, cho phép quyền thông báo để dùng nhắc lịch.

Build bằng terminal:

```powershell
.\gradlew.bat clean assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```
