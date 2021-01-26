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

  return (
    <div
      css={{
        flex: 1,
        backgroundColor: colors.structure.coal,
        padding: spacing.base,
        overflow: 'auto',
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
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
                width: size - spacing.base * 2,
                height: size - spacing.base * 2,
                ':not(:last-of-type)': { marginBottom: spacing.base },
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
