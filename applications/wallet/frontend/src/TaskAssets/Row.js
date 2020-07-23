/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
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
  const [isOpen, setOpen] = useLocalStorageState({
    key: `TaskAssets.${id}`,
    initialValue: false,
  })

  const toggle = () => setOpen({ value: !isOpen })

  return (
    <div
      css={{
        backgroundColor: colors.structure.mattGrey,
        borderBottom: constants.borders.regular.smoke,
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          ':hover': {
            cursor: 'pointer',
            backgroundColor: colors.structure.iron,
          },
        }}
        onClick={toggle}
      >
        <div css={{ width: constants.icons.regular * 3, textAlign: 'center' }}>
          {index}
        </div>

        <div
          css={{
            width: THUMBNAIL_SIZE,
            minWidth: THUMBNAIL_SIZE,
            height: THUMBNAIL_SIZE,
          }}
        >
          <AssetsThumbnail asset={asset} />
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

        <div css={{ width: constants.icons.regular * 2, textAlign: 'center' }}>
          <Button
            aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
            variant={BUTTON_VARIANTS.ICON}
            onClick={toggle}
            css={{ padding: 0 }}
          >
            <ChevronSvg
              height={constants.icons.regular}
              css={{
                transform: isOpen ? 'rotate(-180deg)' : '',
              }}
            />
          </Button>
        </div>
      </div>

      {isOpen && (
        <div
          css={{
            height: MAX_HEIGHT,
            overflow: 'auto',
          }}
        >
          <SuspenseBoundary isTransparent>
            <TaskAssetsMetadata projectId={projectId} assetId={asset.id} />
          </SuspenseBoundary>
        </div>
      )}
    </div>
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
