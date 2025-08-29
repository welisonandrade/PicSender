import UIKit

extension UIImage {
    func resized(maxWidth: CGFloat) -> UIImage? {
        let w = size.width, h = size.height
        guard w > maxWidth else { return self }
        let scale = maxWidth / w
        let newSize = CGSize(width: w * scale, height: h * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: newSize, format: format).image { _ in
            self.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}

