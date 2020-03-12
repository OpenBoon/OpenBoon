import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'

import { reducer, INITIAL_STATE } from './reducer'
import assetShape from '../Asset/shape'

const Assets = ({ assets }) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { thumbnailCount, isMin, isMax } = state

  return (
    <div
      css={{
        flex: 1,
        position: 'relative',
      }}>
      <div
        css={{
          height: '100%',
          display: 'flex',
          flexWrap: 'wrap',
          alignContent: 'flex-start',
          overflowY: 'auto',
          padding: spacing.small,
        }}>
        {assets.map(asset => (
          <AssetsThumbnail
            key={asset.id}
            asset={asset}
            thumbnailCount={thumbnailCount}
          />
        ))}
      </div>

      <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
    </div>
  )
}

Assets.propTypes = {
  assets: PropTypes.arrayOf(assetShape).isRequired,
}

export default Assets
