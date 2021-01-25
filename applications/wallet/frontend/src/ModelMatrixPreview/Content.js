import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { reducer } from '../Resizeable/reducer'

import { PANEL_WIDTH } from '../ModelMatrix/helpers'

const FROM = 0
const SIZE = 28

const ModelMatrixPreviewContent = ({ encodedFilter, projectId }) => {
  const {
    data: { results, count },
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

  return (
    <div
      css={{
        flex: 1,
        // height: '100%',
        height: '0%',
        backgroundColor: colors.structure.coal,
        padding: spacing.small,
        overflow: 'auto',
      }}
    >
      <div
        css={{
          display: 'grid',
          gridTemplateColumns: `repeat(${Math.floor(
            size / 200,
          )}, minmax(200px, 1fr))`,
          gap: spacing.small,
        }}
      >
        {results.map(({ thumbnailUrl, metadata, id }) => {
          const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

          return (
            <div
              key={id}
              css={{
                position: 'relative',
                backgroundColor: colors.structure.mattGrey,
                aspectRatio: '1 / 1',
              }}
            >
              <img
                css={{
                  position: 'absolute',
                  maxWidth: '100%',
                  maxHeight: '100%',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
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
