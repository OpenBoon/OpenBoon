import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { reducer } from '../Resizeable/reducer'

import { PANEL_WIDTH } from './helpers'

const FROM = 0
const SIZE = 28
const PANEL_BORDER_WIDTH = 1
const PANEL_PADDING = 8
const GRID_GAP = 8

const ModelMatrixPreviewContent = ({ encodedFilter, projectId }) => {
  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${encodedFilter}&from=${FROM}&size=${SIZE}`,
  )

  const [{ size }] = useLocalStorage({
    key: `Resizeable.ModelMatrixPreview`,
    reducer,
    initialState: {
      size: PANEL_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  const numColumns = Math.floor((size - PANEL_BORDER_WIDTH) / 200)

  return (
    <div
      css={{
        flex: 1,
        backgroundColor: colors.structure.coal,
        padding: PANEL_PADDING,
        overflow: 'auto',
      }}
    >
      <div
        css={{
          display: 'grid',
          gridTemplateColumns: `repeat(${numColumns}, 1fr)`,
          // make row height match column width
          gridAutoRows:
            (size -
              PANEL_BORDER_WIDTH -
              PANEL_PADDING * 2 -
              GRID_GAP * (numColumns - 1)) /
            numColumns,
          gap: GRID_GAP,
        }}
      >
        {results.map(({ thumbnailUrl, metadata, id }) => {
          const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

          return (
            <div
              key={id}
              title={metadata?.source?.filename}
              css={{
                position: 'relative',
                backgroundColor: colors.structure.mattGrey,
              }}
            >
              <img
                css={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'contain',
                }}
                src={thumbnailSrc}
                alt={metadata?.source?.filename}
              />
            </div>
          )
        })}
      </div>
    </div>
  )
}

ModelMatrixPreviewContent.propTypes = {
  encodedFilter: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
}

export default ModelMatrixPreviewContent
