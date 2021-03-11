import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, typography, colors } from '../Styles'

import chartShape from '../Chart/shape'

import FilterSvg from '../Icons/filter.svg'
import HistogramSvg from '../Icons/histogram.svg'

import {
  encode,
  cleanup,
  decode,
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
} from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'
import { ACTIONS as CHART_ACTIONS } from '../DataVisualization/reducer'

import { CHART_TYPE_FIELDS } from '../ChartForm/helpers'

import Button, { VARIANTS } from '../Button'

const BAR_HEIGHT = 4
const ICON_PADDING = spacing.small

const COLORS = [
  colors.signal.sky.base,
  colors.graph.magenta,
  colors.signal.halloween.base,
  colors.signal.canary.base,
  colors.graph.seafoam,
  colors.graph.rust,
  colors.graph.coral,
  colors.graph.iris,
  colors.graph.marigold,
  colors.graph.magenta,
  colors.signal.grass.base,
]

const ChartFacetContent = ({
  chart: { type, id, attribute, values },
  dispatch: chartDispatch,
}) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  const visuals = encode({
    filters: [
      {
        type,
        id,
        attribute,
        options: { size: parseInt(values, 10) },
      },
    ],
  })

  const q = cleanup({ query })

  const queryString = getQueryString({ query: q, visuals })

  const { data = [] } = useSWR(
    `/api/v1/projects/${projectId}/visualizations/load/${queryString}`,
  )

  const { results = {}, defaultFilterType } =
    data.find((r) => r.id === id) || {}

  const { buckets = [] } = results

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  const filters = decode({ query })

  const { values: { facets = [], labels = [], min = 0, max = 1 } = {} } =
    filters.find((f) => f.attribute === attribute) || {}

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        flex: 1,
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingBottom: spacing.base,
          paddingRight: constants.icons.regular + ICON_PADDING * 2,
          fontFamily: typography.family.condensed,
          color: colors.structure.zinc,
        }}
      >
        <div>LABEL</div>
        <div>COUNT</div>
      </div>

      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          overflowY: 'auto',
          flex: 1,
        }}
      >
        {buckets.map(({ key, docCount = 0 }, index) => {
          const colorIndex = index % COLORS.length

          const offset = Math.ceil((docCount * 100) / largestCount)
          const facetIndex = facets.findIndex((f) => f === key)
          const labelIndex = labels.findIndex((f) => f === key)
          const isSelected = !!(facetIndex + 1) || !!(labelIndex + 1)

          return (
            <li key={key}>
              <Button
                aria-label={key}
                css={{
                  width: '100%',
                  display: 'flex',
                  flexDirection: 'row',
                  color: colors.structure.white,
                  ':hover, &.focus-visible:focus': {
                    backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
                    color: colors.structure.white,
                    svg: { color: colors.structure.white },
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={() => {
                  if (isSelected) return

                  const newValues =
                    defaultFilterType === 'labelConfidence'
                      ? { labels: [key], min, max }
                      : { facets: [key] }

                  filterDispatch({
                    type: FILTER_ACTIONS.ADD_VALUE,
                    payload: {
                      pathname,
                      projectId,
                      filter: {
                        type: defaultFilterType,
                        attribute,
                        values: newValues,
                      },
                      query,
                    },
                  })
                }}
              >
                <div css={{ width: '100%', display: 'flex' }}>
                  <div
                    css={{
                      display: 'flex',
                      flexDirection: 'column',
                      flex: 1,
                      overflow: 'hidden',
                    }}
                  >
                    <div css={{ flex: 1, display: 'flex' }}>
                      <div
                        css={{
                          width: `${offset}%`,
                          backgroundColor: COLORS[colorIndex],
                        }}
                      />
                      <div
                        css={{
                          height: BAR_HEIGHT,
                          width: `${100 - offset}%`,
                          borderTop: constants.borders.regular.smoke,
                        }}
                      />
                    </div>
                    <div
                      css={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        padding: spacing.base,
                        fontFamily: typography.family.mono,
                        fontSize: typography.size.small,
                        lineHeight: typography.height.small,
                      }}
                    >
                      <div
                        css={{
                          overflow: 'hidden',
                          whiteSpace: 'nowrap',
                          textOverflow: 'ellipsis',
                        }}
                      >
                        {key}
                      </div>
                      <div css={{ paddingLeft: spacing.base }}>{docCount}</div>
                    </div>
                  </div>
                  <div
                    css={{
                      marginTop: BAR_HEIGHT,
                      padding: ICON_PADDING,
                      color: colors.structure.transparent,
                      display: 'flex',
                      justifyContent: 'center',
                    }}
                  >
                    <FilterSvg height={constants.icons.regular} />
                  </div>
                </div>
              </Button>
            </li>
          )
        })}
      </ul>

      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          paddingTop: spacing.normal,
        }}
      >
        <Button
          aria-label="Add Filter"
          variant={VARIANTS.MICRO}
          onClick={() => {
            filterDispatch({
              type: FILTER_ACTIONS.ADD_FILTER,
              payload: {
                pathname,
                projectId,
                filter: { type: defaultFilterType, attribute, values: {} },
                query,
              },
            })
          }}
        >
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <div css={{ display: 'flex', paddingRight: spacing.small }}>
              <FilterSvg height={constants.icons.regular} />
            </div>
            Add Filter
          </div>
        </Button>

        {CHART_TYPE_FIELDS.histogram.includes(defaultFilterType) && (
          <>
            <div css={{ width: spacing.base }} />

            <Button
              aria-label="Add Histogram Visualization"
              variant={VARIANTS.MICRO}
              onClick={() => {
                chartDispatch({
                  type: CHART_ACTIONS.CREATE,
                  payload: {
                    type: 'histogram',
                    attribute,
                    values: 10,
                  },
                })
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <HistogramSvg
                  height={constants.icons.regular}
                  css={{ paddingRight: spacing.base }}
                />
                Add Histogram Visualization
              </div>
            </Button>
          </>
        )}
      </div>
    </div>
  )
}

ChartFacetContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartFacetContent
