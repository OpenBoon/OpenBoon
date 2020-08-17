import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { constants, spacing } from '../Styles'

import chartShape from '../Chart/shape'

import { ACTIONS, dispatch } from '../Filters/helpers'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FilterSvg from '../Icons/filter.svg'

const ChartHistogramContent = ({
  chart: { type, id, attribute, scale, values },
}) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
      }}
    >
      <div css={{ flex: 1 }}>
        <div>{`type: ${type}`}</div>
        <div>{`id: ${id}`}</div>
        <div>{`attribute: ${attribute}`}</div>
        <div>{`scale: ${scale}`}</div>
        <div>{`values: ${values}`}</div>
      </div>

      <div css={{ height: spacing.comfy }} />

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
