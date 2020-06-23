import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, typography, colors } from '../Styles'

import chartShape from '../Chart/shape'

import { encode, decode, ACTIONS, dispatch } from '../Filters/helpers'
import Button, { VARIANTS } from '../Button'

import FilterSvg from '../Icons/filter.svg'

const BAR_HEIGHT = 4
const ICON_SIZE = 20
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

const ChartFacetContent = ({ chart: { type, attribute } }) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  const encodedFilter = encode({ filters: { type, attribute } })

  const { data } = useSWR(
    // TODO: Update endpoint
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  const { results } = data || {}

  const { buckets = [] } = results || {}

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  const filters = decode({ query })

  const { values: { facets = [] } = {} } =
    filters.find((f) => f.attribute === attribute) || {}

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingBottom: spacing.base,
          paddingRight: ICON_SIZE + ICON_PADDING * 2,
          fontFamily: typography.family.condensed,
          color: colors.structure.zinc,
        }}
      >
        <div>LABEL</div>
        <div>COUNT</div>
      </div>

      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount = 0 }, index) => {
          const colorIndex = index % COLORS.length

          const offset = Math.ceil((docCount * 100) / largestCount)
          const facetIndex = facets.findIndex((f) => f === key)
          const isSelected = !!(facetIndex + 1)

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
                    backgroundColor: colors.signal.electricBlue.background,
                    color: colors.structure.white,
                    svg: {
                      color: colors.structure.white,
                    },
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={() => {
                  if (isSelected) return

                  dispatch({
                    type: ACTIONS.ADD_VALUE,
                    payload: {
                      pathname,
                      projectId,
                      filter: {
                        type,
                        attribute,
                        values: { facets: [...facets, key] },
                      },
                      query,
                    },
                  })
                }}
              >
                <div css={{ width: '100%' }}>
                  <div css={{ display: 'flex' }}>
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
                        borderTop: constants.borders.divider,
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
                    <div>{key}</div>
                    <div>{docCount}</div>
                  </div>
                </div>
                <div
                  css={{
                    marginTop: BAR_HEIGHT,
                    padding: ICON_PADDING,
                    color: colors.transparent,
                    display: 'flex',
                    justifyContent: 'center',
                  }}
                >
                  <FilterSvg width={ICON_SIZE} />
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
        }}
      >
        <Button
          aria-label="Add Filter"
          variant={VARIANTS.NEUTRAL}
          onClick={() => {
            dispatch({
              type: ACTIONS.ADD_FILTER,
              payload: {
                pathname,
                projectId,
                filter: { type, attribute, values: {} },
                query,
              },
            })
          }}
          css={{
            padding: spacing.normal,
            display: 'flex',
            color: colors.structure.steel,
            fontFamily: typography.family.condensed,
            ':hover, &.focus-visible:focus': {
              color: colors.structure.white,
            },
          }}
        >
          <div css={{ display: 'flex', alignItems: 'center' }}>
            <div css={{ display: 'flex', paddingRight: spacing.small }}>
              <FilterSvg width={ICON_SIZE} />
            </div>
            Add Field Filter
          </div>
        </Button>
      </div>
    </div>
  )
}

ChartFacetContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartFacetContent
