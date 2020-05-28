import { useRef, forwardRef, useEffect } from 'react'
import { FixedSizeGrid } from 'react-window'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import AssetsThumbnail from './Thumbnail'

const PADDING_SIZE = spacing.small

const AssetsGrid = ({
  innerRef,
  height,
  width,
  items,
  columnCount,
  onItemsRendered,
  selectedRow,
}) => {
  const gridRef = useRef()

  const { parentElement } = (innerRef && innerRef.current) || {}
  const { offsetWidth = 0, clientWidth = 0 } = parentElement || {}
  const adjustedWidth = width - PADDING_SIZE * 2
  const scrollbarSize = offsetWidth - clientWidth
  const thumbnailSize = Math.max(100, adjustedWidth / columnCount)
  const rowCount = Math.ceil(items.length / columnCount)
  const hasVerticalScrollbar = rowCount * thumbnailSize > height
  const scrollbarBuffer = hasVerticalScrollbar ? scrollbarSize : 0
  const adjustedThumbnailSize = Math.max(
    100,
    (adjustedWidth - scrollbarBuffer) / columnCount,
  )

  useEffect(() => {
    if (selectedRow) {
      // align="smart" - If the item is already visible, don't scroll at all.
      // If it is less than one viewport away, scroll as little as possible
      // so that it becomes visible. If it is more than one viewport away,
      // scroll so that it is centered within the grid.
      gridRef.current.scrollToItem({ align: 'smart', rowIndex: selectedRow })
    }
  }, [selectedRow])

  return (
    <FixedSizeGrid
      innerRef={innerRef}
      ref={gridRef}
      onItemsRendered={({
        visibleRowStartIndex,
        visibleRowStopIndex,
        visibleColumnStartIndex,
        visibleColumnStopIndex,
      }) => {
        const visibleStartIndex =
          visibleRowStartIndex * columnCount + visibleColumnStartIndex

        const visibleStopIndex =
          visibleRowStopIndex * columnCount + visibleColumnStopIndex

        onItemsRendered({
          visibleStartIndex,
          visibleStopIndex,
        })
      }}
      columnCount={columnCount}
      columnWidth={adjustedThumbnailSize}
      rowHeight={adjustedThumbnailSize}
      rowCount={rowCount}
      width={width}
      height={height - PADDING_SIZE / 2}
      innerElementType={forwardRef(({ style, ...rest }, elementRef) => (
        <div
          ref={elementRef}
          style={{
            ...style,
            width: `${parseFloat(style.width) + PADDING_SIZE * 2}px`,
            height: `${parseFloat(style.height) + PADDING_SIZE * 2}px`,
          }}
          // eslint-disable-next-line react/jsx-props-no-spreading
          {...rest}
        />
      ))}
    >
      {({ columnIndex, rowIndex, style }) => {
        const index = columnIndex + rowIndex * columnCount

        if (!items[index]) return null

        return (
          <div
            style={{
              ...style,
              top: `${parseFloat(style.top) + PADDING_SIZE}px`,
              left: `${parseFloat(style.left) + PADDING_SIZE}px`,
            }}
          >
            <AssetsThumbnail asset={items[index]} />
          </div>
        )
      }}
    </FixedSizeGrid>
  )
}

AssetsGrid.propTypes = {
  innerRef: PropTypes.element.isRequired,
  height: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
  items: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  columnCount: PropTypes.number.isRequired,
  onItemsRendered: PropTypes.func.isRequired,
  selectedRow: PropTypes.number.isRequired,
}

export default AssetsGrid
