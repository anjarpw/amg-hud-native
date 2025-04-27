import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class CustomLayoutGroup(context: Context) : ViewGroup(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure the children views and determine the layout's size.
        // This is where you define how the children should be measured
        // based on the given constraints.

        var totalWidth = 0
        var totalHeight = 0

        // Iterate through each child view
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // মেপে দেখুন child view টি
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            // Calculate total width and height
            totalWidth += child.measuredWidth
            totalHeight += child.measuredHeight
        }

        // Choose final dimensions for the layout.
        //  Here, we're using the total width and height of the children,
        //  but you can implement your own logic.
        setMeasuredDimension(
            resolveSize(totalWidth, widthMeasureSpec),
            resolveSize(totalHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Position the child views within the layout.
        // This is where you define the position of each child view
        // using the values provided (l, t, r, b).

        var currentTop = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Place the child view at the calculated position
            child.layout(l, currentTop, l + child.measuredWidth, currentTop + child.measuredHeight)
            currentTop += child.measuredHeight // Example: Stack children vertically
        }
    }
}