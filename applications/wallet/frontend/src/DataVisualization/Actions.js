import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import PlusSvg from '../Icons/plus.svg'

import { ACTIONS } from './reducer'

const ICON_SIZE = 20

const DataVisualizationActions = ({ dispatch, setIsCreating }) => {
  return (
    <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
      <Button
        aria-label="Add Chart"
        variant={VARIANTS.SECONDARY_SMALL}
        onClick={() => {
          setIsCreating(true)
        }}
        css={{ display: 'flex', marginRight: spacing.normal }}
      >
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <div css={{ display: 'flex', paddingRight: spacing.small }}>
            <PlusSvg width={ICON_SIZE} />
          </div>
          Add Chart
        </div>
      </Button>

      <Button
        variant={VARIANTS.SECONDARY_SMALL}
        onClick={() => {
          dispatch({ type: ACTIONS.CLEAR })

          setIsCreating(true)
        }}
        css={{ marginRight: spacing.normal }}
      >
        Delete All
      </Button>
    </div>
  )
}

DataVisualizationActions.propTypes = {
  dispatch: PropTypes.func.isRequired,
  setIsCreating: PropTypes.func.isRequired,
}

export default DataVisualizationActions
