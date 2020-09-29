import PropTypes from 'prop-types'

import { useLocalStorage } from '../LocalStorage/helpers'

import { typography, colors, spacing, constants } from '../Styles'

import AssetsThumbnail from '../Assets/Thumbnail'
import SuspenseBoundary from '../SuspenseBoundary'

import ChevronSvg from '../Icons/chevron.svg'

import TaskAssetsMetadata from './Metadata'

const THUMBNAIL_SIZE = 100
const MAX_HEIGHT = 600

const TaskAssetsRow = ({
  projectId,
  index,
  asset,
  asset: {
    id,
    metadata: {
      source: { filename },
    },
  },
}) => {
  const [isOpen, setOpen] = useLocalStorage({
    key: `TaskAssets.${id}`,
    initialState: false,
  })

  return (
    <details
      css={{
        backgroundColor: colors.structure.mattGrey,
        borderBottom: constants.borders.regular.smoke,
      }}
      open={isOpen}
      onToggle={({ target: { open } }) => setOpen({ value: open })}
    >
      <summary
        aria-label={filename}
        css={{
          listStyleType: 'none',
          '::-webkit-details-marker': { display: 'none' },
          ':hover': {
            cursor: 'pointer',
            backgroundColor: colors.structure.iron,
            svg: { color: colors.structure.white },
          },
        }}
      >
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <div
            css={{ width: constants.icons.regular * 3, textAlign: 'center' }}
          >
            {index}
          </div>

          <div
            css={{
              width: THUMBNAIL_SIZE,
              minWidth: THUMBNAIL_SIZE,
              height: THUMBNAIL_SIZE,
            }}
          >
            <AssetsThumbnail asset={asset} isActive={false} />
          </div>

          <h4
            css={{
              flex: 1,
              display: 'flex',
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              fontWeight: typography.weight.regular,
              paddingLeft: spacing.moderate,
            }}
          >
            {filename}
          </h4>

          <div css={{ width: constants.icons.regular * 2 }}>
            <ChevronSvg
              height={constants.icons.regular}
              css={{
                color: colors.structure.steel,
                transform: isOpen ? 'rotate(-180deg)' : '',
              }}
            />
          </div>
        </div>
      </summary>

      {isOpen && (
        <div
          css={{
            height: MAX_HEIGHT,
            overflow: 'auto',
            backgroundColor: colors.structure.coal,
          }}
        >
          <SuspenseBoundary isTransparent>
            <TaskAssetsMetadata projectId={projectId} assetId={asset.id} />
          </SuspenseBoundary>
        </div>
      )}
    </details>
  )
}

TaskAssetsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  index: PropTypes.number.isRequired,
  asset: PropTypes.shape({
    id: PropTypes.string.isRequired,
    metadata: PropTypes.shape({
      source: PropTypes.shape({
        path: PropTypes.string,
        filename: PropTypes.string,
        extension: PropTypes.string,
        mimetype: PropTypes.string,
      }),
    }),
    thumbnailUrl: PropTypes.string.isRequired,
    assetStyle: PropTypes.oneOf(['image', 'video', 'document']),
    videoLength: PropTypes.number,
    videoProxyUrl: PropTypes.string,
  }).isRequired,
}

export default TaskAssetsRow
