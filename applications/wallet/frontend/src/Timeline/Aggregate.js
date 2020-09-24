import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

import { filterDetections } from './helpers'

import { ACTIONS } from './reducer'

const TimelineAggregate = ({
  timelineHeight,
  detections,
  settings,
  dispatch,
}) => {
  const filteredDetections = filterDetections({ detections, settings })

  const isAllVisible = Object.values(settings.modules).every(
    ({ isVisible }) => isVisible === true,
  )

  return (
    <div
      css={{
        display: 'flex',
        marginLeft: -constants.timeline.modulesWidth,
        borderTop: constants.borders.regular.smoke,
        height: constants.timeline.rulerRowHeight,
      }}
    >
      <div css={{ width: constants.timeline.modulesWidth }}>
        <Menu
          open="bottom-center"
          button={({ onBlur, onClick, isMenuOpen }) => (
            <MenuButton
              onBlur={onBlur}
              onClick={onClick}
              legend={`Detections (${filteredDetections.length})`}
              style={{
                '&,&:hover,&:visited': {
                  backgroundColor: isMenuOpen
                    ? colors.structure.steel
                    : colors.structure.smoke,
                },
                '&:hover': {
                  backgroundColor: colors.structure.steel,
                },
                '&[aria-disabled=true]': {
                  backgroundColor: colors.structure.smoke,
                },
                marginBottom: 0,
                borderRadius: 0,
                height: '100%',
              }}
            />
          )}
        >
          {() => (
            <div
              // eslint-disable-next-line
              tabIndex="1"
              css={{
                backgroundColor: colors.structure.iron,
                maxHeight:
                  timelineHeight - constants.timeline.rulerRowHeight * 2,
                overflowY: 'auto',
              }}
            >
              <div css={{ borderBottom: constants.borders.medium.steel }}>
                <Checkbox
                  key={isAllVisible}
                  variant={CHECKBOX_VARIANTS.MENU}
                  option={{
                    value: 'all',
                    label: 'All',
                    initialValue: isAllVisible,
                    isDisabled: false,
                  }}
                  onClick={() => {
                    dispatch({
                      type: ACTIONS.TOGGLE_VISIBLE_ALL,
                      payload: { detections },
                    })
                  }}
                />
              </div>

              {detections.map(({ name, predictions }) => (
                <Checkbox
                  key={`${name}.${settings.modules[name]?.isVisible}`}
                  variant={CHECKBOX_VARIANTS.MENU}
                  option={{
                    value: name,
                    label: name,
                    legend: `(${predictions.length})`,
                    initialValue: settings.modules[name]?.isVisible !== false,
                    isDisabled: false,
                  }}
                  onClick={() => {
                    dispatch({
                      type: ACTIONS.TOGGLE_VISIBLE,
                      payload: { name },
                    })
                  }}
                />
              ))}
            </div>
          )}
        </Menu>
      </div>
    </div>
  )
}

TimelineAggregate.propTypes = {
  timelineHeight: PropTypes.number.isRequired,
  detections: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    modules: PropTypes.shape({}).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineAggregate
