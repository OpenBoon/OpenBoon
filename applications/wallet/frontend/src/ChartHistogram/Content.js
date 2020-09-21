import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import chartShape from '../Chart/shape'

import FilterSvg from '../Icons/filter.svg'
import FacetSvg from '../Icons/facet.svg'

import {
  ACTIONS as FILTER_ACTIONS,
  cleanup,
  dispatch as filterDispatch,
  encode,
} from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'
import { ACTIONS as CHART_ACTIONS } from '../DataVisualization/reducer'

import Button, { VARIANTS } from '../Button'

import { formatTitle } from './helpers'

const ChartHistogramContent = ({
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

  const minScore = (buckets.length && buckets[0].key) || 0
  const maxScore = (buckets.length && buckets[buckets.length - 1].key) || 1

  const { docCount: largestCount = 1 } = (buckets.length &&
    [...buckets].sort((a, b) => b.docCount - a.docCount)[0]) || [{}]

  const barWidth = 100 / buckets.length
  const marginWidth = 100 / buckets.length / 3

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
      }}
    >
      <div
        css={{
          textTransform: 'uppercase',
          fontFamily: typography.family.condensed,
          color: colors.structure.zinc,
          paddingBottom: spacing.spacious,
        }}
      >
        Label Prediction Confidence
      </div>

      <div css={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <div
          css={{
            display: 'flex',
            height: '100%',
            alignItems: 'baseline',
            paddingBottom: spacing.base,
          }}
        >
          {buckets.map(({ key, docCount }, index) => {
            const offset = Math.ceil((docCount * 100) / largestCount)

            return (
              <div
                key={key}
                title={formatTitle({ buckets, index })}
                css={{
                  width: `${barWidth}%`,
                  height: `${offset}%`,
                  '&:not(:last-of-type)': {
                    marginRight: `${marginWidth}%`,
                  },
                  backgroundColor: colors.key.one,
                  ':hover': {
                    backgroundColor: colors.key.two,
                  },
                }}
              />
            )
          })}
        </div>
        <div
          css={{
            height: spacing.hairline,
            backgroundColor: colors.structure.smoke,
          }}
        />
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            fontFamily: typography.family.mono,
            paddingTop: spacing.moderate,
          }}
        >
          <span>{minScore.toFixed(2)}</span>
          <span css={{ fontFamily: typography.family.regular }}>
            Confidence Score Range
          </span>
          <span>{maxScore.toFixed(2)}</span>
        </div>
      </div>

      <div css={{ display: 'flex', justifyContent: 'center' }}>
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
          <div css={{ display: 'flex', alignItems: 'center' }}>
            <FilterSvg
              height={constants.icons.regular}
              css={{ paddingRight: spacing.base }}
            />
            Add Filter
          </div>
        </Button>

        <div css={{ width: spacing.base }} />

        <Button
          aria-label="Add Facet Visualization"
          variant={VARIANTS.MICRO}
          onClick={() => {
            chartDispatch({
              type: CHART_ACTIONS.CREATE,
              payload: {
                type: 'facet',
                attribute,
                values: 10,
              },
            })
          }}
        >
          <div css={{ display: 'flex', alignItems: 'center' }}>
            <FacetSvg
              height={constants.icons.regular}
              css={{ paddingRight: spacing.base }}
            />
            Add Facet Visualization
          </div>
        </Button>
      </div>
    </div>
  )
}

ChartHistogramContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartHistogramContent
