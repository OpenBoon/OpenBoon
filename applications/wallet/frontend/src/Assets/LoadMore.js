import PropTypes from 'prop-types'

import { constants, colors } from '../Styles'

import Button, { VARIANTS } from '../Button'

/* istanbul ignore next */
const AssetsLoadMore = ({
  pageCount,
  isReachingEnd,
  isLoadingMore,
  loadMore,
}) => {
  if ((isLoadingMore && pageCount === 1) || isReachingEnd) return null

  const label = isLoadingMore ? '...' : 'load more'

  return (
    <div
      className="container"
      css={{
        height: 0,
        position: 'relative',
        minWidth: 100,
        minHeight: 100,
      }}
    >
      <div
        css={{
          border: constants.borders.assetInactive,
          width: '100%',
          height: '100%',
          position: 'absolute',
        }}
      >
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={loadMore}
          isDisabled={isReachingEnd || isLoadingMore}
          css={{
            width: '100%',
            height: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            background: colors.structure.mattGrey,
            overflow: 'hidden',
          }}
        >
          {label}
        </Button>
      </div>
    </div>
  )
}

AssetsLoadMore.propTypes = {
  pageCount: PropTypes.number.isRequired,
  isReachingEnd: PropTypes.bool.isRequired,
  isLoadingMore: PropTypes.bool.isRequired,
  loadMore: PropTypes.func.isRequired,
}

export default AssetsLoadMore
