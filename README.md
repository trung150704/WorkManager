HEAD
# WorkManager - App Nhắc Nhở Uống Thuốc & Công Việc

Ứng dụng Android nhắc nhở lịch uống thuốc, theo dõi kho thuốc và quản lý công việc/học tập lặp lại, được phát triển bằng **Jetpack Compose**, **Material 3**, **Room Database**, **WorkManager** và **Hilt**.

## 📖 Tài liệu chi tiết
Xem tài liệu review chi tiết cho đội ngũ phát triển tại: **[PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)**

## 🚀 Tính năng chính
- **Nhắc nhở uống thuốc & công việc**: Hỗ trợ lặp lại theo thứ trong tuần, liều lượng và ghi chú.
- **Theo dõi kho thuốc & Cảnh báo sắp hết**: Tự động trừ số lượng thuốc khi chọn "Đã uống" từ thông báo và đưa ra cảnh báo khi còn $\le 3$ viên.
- **Tương tác nhanh qua Thông báo**: Các nút hành động *Đã uống*, *Hoàn thành*, *Hoãn 10 phút*, *Bỏ qua* ngay trên thanh thông báo.
- **Thống kê & Nhớ nhật ký**: Biểu đồ tuần tính tỷ lệ tuân thủ % và theo dõi lịch sử tương tác.
- **Tự động đối soát & Tự khôi phục lịch**: Tác vụ ngầm 12h đánh dấu lần bỏ lỡ (`MISSED`) và `BootReceiver` tự đặt lại lịch khi khởi động lại máy.

## 🛠 Công nghệ sử dụng
- **Kotlin**, **Jetpack Compose**, **Material 3**
- **Hilt (Dagger)** cho Dependency Injection
- **Room Database** & **KSP**
- **WorkManager** & **Hilt Work**
- **Navigation Compose**, **Coroutines & Flow**

# WorkManager
7e17545df0d3bb23afa5c3cb3452c9615a5df016
