import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import chartShape from '../Chart/shape'

import { ACTIONS, cleanup, dispatch, encode } from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { formatTitle } from './helpers'

import FilterSvg from '../Icons/filter.svg'

const ChartHistogramContent = ({ chart: { type, id, attribute, values } }) => {
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
        fieldType: 'labelConfidence',
        options: { size: parseInt(values, 10) },
      },
    ],
  })

  const q = cleanup({ query })

  const queryString = getQueryString({ query: q, visuals })

  const { data = [] } = useSWR(
    `/api/v1/projects/${projectId}/visualizations/load/${queryString}`,
  )

  const { results = {} } = data.find((r) => r.id === id) || {}

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
          variant={BUTTON_VARIANTS.MICRO}
          onClick={() => {
            dispatch({
              type: ACTIONS.ADD_FILTER,
              payload: {
                pathname,
                projectId,
                filter: { type: 'labelConfidence', attribute, values: {} },
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
      </div>
    </div>
  )
}

ChartHistogramContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartHistogramContent
