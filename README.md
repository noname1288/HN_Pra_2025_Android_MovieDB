## **1. Bảng quan hệ các class**

| Class          | Thuộc tính chính                                                                                                | Liên kết với class khác                                          | Ý nghĩa                                                                                                  |
| -------------- | --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **Category**   | `id`, `name`, `slug`                                                                                            | Được dùng trong `Item.category`, `Movie.category`                | Thông tin thể loại phim (ví dụ: Hành động, Hài, Tình cảm...)                                             |
| **Country**    | `id`, `name`, `slug`                                                                                            | Được dùng trong `Item.country`, `Movie.country`                  | Thông tin quốc gia sản xuất phim (ví dụ: Mỹ, Hàn Quốc, Việt Nam)                                         |
| **Data**       | `items`, `params`, `titlePage`, `type_list`                                                                     | `items` chứa `List<Item>`, `params` là `Params`                  | Gói dữ liệu trả về trong một số API, bao gồm danh sách phim (`items`) và tham số lọc (`params`)          |
| **Episode**    | `server_data`, `server_name`                                                                                    | `server_data` chứa `List<ServerData>`                            | Thông tin 1 tập phim, gồm tên server phát và các link phát                                               |
| **Item**       | `_id`, `category`, `country`, `poster_url`, `thumb_url`, `year`, ...                                            | `category` chứa `List<Category>`, `country` chứa `List<Country>` | Thông tin tóm tắt phim (dùng trong danh sách phim)                                                       |
| **Movie**      | `_id`, `actor`, `category`, `country`, `content`, `poster_url`, `thumb_url`, `trailer_url`, `view`, `year`, ... | `category` chứa `List<Category>`, `country` chứa `List<Country>` | Thông tin chi tiết phim (đầy đủ hơn `Item`, gồm diễn viên, đạo diễn, nội dung, trailer, số lượt xem,...) |
| **Pagination** | `currentPage`, `totalItems`, `totalItemsPerPage`, `totalPages`                                                  | Được dùng trong `Params.pagination`                              | Thông tin phân trang (trang hiện tại, tổng số item, số item mỗi trang, tổng số trang)                    |
| **Params**     | `filterCategory`, `filterCountry`, `filterType`, `filterYear`, `pagination`, `slug`, `sortField`, `sortType`    | `pagination` là `Pagination`                                     | Tham số lọc & sắp xếp dữ liệu trong API (thể loại, quốc gia, năm, phân trang,...)                        |
| **ServerData** | `filename`, `link_embed`, `link_m3u8`, `name`, `slug`                                                           | Được dùng trong `Episode.server_data`                            | Thông tin cụ thể về file/tập phim trên server, gồm link nhúng và link M3U8 để phát                       |

---

## **2. Mối quan hệ tổng thể**

```
Data
 ├─ items : List<Item>
 │    ├─ category : List<Category>
 │    ├─ country  : List<Country>
 │
 └─ params : Params
      └─ pagination : Pagination

Movie
 ├─ category : List<Category>
 ├─ country  : List<Country>

Episode
 └─ server_data : List<ServerData>
```

---

## **3. Giải thích luồng dữ liệu khi gọi API**

1. **API danh sách phim** → trả về `Data` hoặc `List<Item>` kèm `Pagination`.
2. **API chi tiết phim** → trả về `Movie` (chi tiết phim) + `List<Episode>` (tập phim).
3. **Category** và **Country** → dùng để lọc hoặc hiển thị thông tin phim.
4. **ServerData** → chứa link thực tế để phát video.
5. **Params** và **Pagination** → phục vụ phân trang và lọc dữ liệu.

---

## **4. Tính năng nổi bật**
- Tính năng **Tạo phòng xem phim (Watch Party)**  cho phép bạn bè cùng xem một bộ phim và **đồng bộ** với nhau, dù đang ở bất cứ đâu.

| Tính Năng | Mô Tả Lợi Ích Cho Người Dùng |
| :--- | :--- |
| **Đồng Bộ Realtime** |  Khi một người **tạm dừng (Pause)**, **phát tiếp (Play)**, hoặc **tua nhanh/tua lại**, video sẽ được đồng bộ ngay lập tức cho tất cả mọi người |
| **Chat Realtime (Thời Gian Thực)** | **Trao đổi cảm xúc, bình luận** về các tình tiết phim ngay lập tức trong phòng chat riêng|
| **Quản Lý Thành Viên** | **Chủ phòng (Host)** có đầy đủ quyền để: * **Mời** thêm bạn bè, * **Chấp nhận/Từ chối** yêu cầu tham gia,  và * **Loại bỏ** thành viên không phù hợp (Kick member) |
| **Dễ Dàng Thiết Lập** | Chỉ cần một cú chạm để **tạo phòng**, chia sẻ đường link mời, và bắt đầu cuộc vui xem phim ngay lập tức. |

- Hơn nữa, với mục tiêu **"Native First"**, ứng dụng cố gắng giảm thiểu sự phụ thuộc vào các thư viện bên ngoài (Retrofit, ViewModel, LiveData,...) trừ một số thư viện đặc biệt (Glide, Exoplayer)

## **5. Demo**
| Feature | Preview |
|----------|----------|
| 🕹️ Watch Movie | [![Watch Movie]](https://drive.google.com/drive/folders/1gI9HWjq2If7PrpEJndytseBJlsp5OLQg?usp=sharing) |
| ❤️ Room_Chat | [![Room_Chat]](https://drive.google.com/drive/folders/1gI9HWjq2If7PrpEJndytseBJlsp5OLQg?usp=sharing) |


