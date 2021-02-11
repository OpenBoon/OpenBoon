import { useSWRInfinite } from 'swr'
import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { reducer } from '../Resizeable/reducer'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { PANEL_WIDTH } from './helpers'

const SIZE = 28
const PANEL_BORDER_WIDTH = 1

const ModelMatrixPreviewContent = ({ encodedFilter, projectId }) => {
  /* istanbul ignore next */
  const { data, size, setSize } = useSWRInfinite(
    (pageIndex, previousPageData) => {
      if (previousPageData && !previousPageData.next) return null

      const from = pageIndex * SIZE

      return `/api/v1/projects/${projectId}/searches/query/?query=${encodedFilter}&from=${from}&size=${SIZE}`
    },
    undefined,
    {
      suspense: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { count } = (data && data[0]) || {}
  const results = Array.isArray(data) ? data.flatMap(({ results: r }) => r) : []

  useLocalStorage({
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
          display: 'grid',
          gridTemplateColumns: `repeat(auto-fit, minmax(${
            PANEL_WIDTH - PANEL_BORDER_WIDTH - spacing.base * 2
          }px, 1fr))`,
          gap: spacing.base,
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
                paddingBottom: '100%',
                backgroundColor: colors.structure.mattGrey,
              }}
            >
              <img
                css={{
                  position: 'absolute',
                  top: 0,
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
      {count && count > results.length && (
        <Button
          variant={BUTTON_VARIANTS.PRIMARY}
          css={{ marginTop: spacing.base }}
          onClick={() => setSize(size + 1)}
        >
          Load More
        </Button>
      )}
    </div>
  )
}

ModelMatrixPreviewContent.propTypes = {
  encodedFilter: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
}

export default ModelMatrixPreviewContent
