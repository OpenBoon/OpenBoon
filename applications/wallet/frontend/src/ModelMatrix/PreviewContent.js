import { useSWRInfinite } from 'swr'
import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { reducer } from '../Resizeable/reducer'

import Loading from '../Loading'
import ErrorSvg from '../Icons/error.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { PANEL_WIDTH } from './helpers'

const SIZE = 28
const PANEL_BORDER_WIDTH = 1

const ModelMatrixPreviewContent = ({
  moduleName,
  encodedFilter,
  projectId,
}) => {
  /* istanbul ignore next */
  const { data, error, size, setSize } = useSWRInfinite(
    (pageIndex, previousPageData) => {
      if (previousPageData && !previousPageData.next) return null

      const from = pageIndex * SIZE

      return `/api/v1/projects/${projectId}/searches/query/?fields=analysis.${moduleName}&query=${encodedFilter}&from=${from}&size=${SIZE}`
    },
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

  if (!data && !error) {
    return <Loading />
  }

  if (error) {
    return (
      <div
        css={{
          height: '100%',
          backgroundColor: colors.structure.coal,
          padding: spacing.base,
        }}
      >
        <div
          css={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            color: colors.structure.steel,
            backgroundColor: colors.structure.lead,
            lineHeight: typography.height.regular,
          }}
        >
          <ErrorSvg width={604} css={{ maxWidth: '80%' }} />
          <br /> Hmmm, something went wrong.
          <br /> Please try refreshing.
        </div>
      </div>
    )
  }

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
        {results.length === 0 ? (
          <div
            css={{
              padding: spacing.moderate,
              fontStyle: typography.style.italic,
            }}
          >
            No predicted assets.
          </div>
        ) : (
          results.map(({ thumbnailUrl, metadata, id }) => {
            const { pathname: thumbnailSrc } = new URL(thumbnailUrl)
            const confidenceScore =
              metadata?.analysis &&
              metadata?.analysis[moduleName]?.predictions &&
              metadata?.analysis[moduleName]?.predictions[0]?.score

            return (
              <div key={id}>
                <div
                  title={metadata?.source?.filename}
                  css={{
                    position: 'relative',
                    paddingTop: '100%',
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
                <div
                  css={{
                    padding: spacing.base,
                    fontWeight: typography.weight.bold,
                    backgroundColor: colors.structure.mattGrey,
                  }}
                >
                  <span
                    css={{
                      color: colors.structure.zinc,
                      fontFamily: typography.family.condensed,
                      fontWeight: typography.weight.regular,
                    }}
                  >
                    Confidence:{' '}
                  </span>
                  {confidenceScore}
                </div>
              </div>
            )
          })
        )}
      </div>
      {!!count && count > results.length && (
        <div css={{ display: 'flex', justifyContent: 'center' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            css={{ marginTop: spacing.base }}
            onClick={() => setSize(size + 1)}
          >
            Load More
          </Button>
        </div>
      )}
    </div>
  )
}

ModelMatrixPreviewContent.propTypes = {
  moduleName: PropTypes.string.isRequired,
  encodedFilter: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
}

export default ModelMatrixPreviewContent
