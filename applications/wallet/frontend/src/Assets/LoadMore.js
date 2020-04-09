/* istanbul ignore next */
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const AssetsLoadMore = ({ isReachingEnd, isLoadingMore, loadMore }) => {
  /* istanbul ignore next */
  const label = isLoadingMore ? '. . .' : 'load more'
  return (
    <div css={{ paddingLeft: spacing.base, paddingRight: spacing.base }}>
      {!isReachingEnd && (
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={loadMore}
          isDisabled={isReachingEnd || isLoadingMore}
          style={{ width: '100%' }}
        >
          {label}
        </Button>
      )}
    </div>
  )
}

AssetsLoadMore.propTypes = {
  isReachingEnd: PropTypes.bool.isRequired,
  isLoadingMore: PropTypes.bool.isRequired,
  loadMore: PropTypes.func.isRequired,
}

export default AssetsLoadMore
