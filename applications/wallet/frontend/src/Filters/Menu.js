import { useState } from 'react'
import PropTypes from 'prop-types'

import { spacing, typography, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from './helpers'

import FiltersMenuOption from './MenuOption'

const FiltersMenu = ({
  projectId,
  assetId,
  filters,
  fields,
  setIsMenuOpen,
}) => {
  const [newFilters, setNewFilters] = useState({})

  const onClick = ({ type, attribute }) => (value) => {
    if (value) {
      setNewFilters((nF) => ({ ...nF, [attribute]: { type, attribute } }))
    } else {
      setNewFilters((nF) => {
        const { [attribute]: filterToRemove, ...rest } = nF
        return rest
      })
    }
  }

  return (
    <div
      css={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        flex: 1,
      }}
    >
      <div css={{ overflowX: 'auto' }}>
        {Object.entries(fields).map(([key, value]) =>
          Array.isArray(value) ? null : (
            <Accordion
              key={key}
              variant={ACCORDION_VARIANTS.PANEL}
              title={key}
              isInitiallyOpen
            >
              <div
                css={{
                  padding: spacing.normal,
                  paddingTop: spacing.base,
                  paddingBottom: spacing.base,
                }}
              >
                {Object.entries(value).map(([subKey, subValue], index, arr) =>
                  Array.isArray(subValue) ? (
                    <FiltersMenuOption
                      key={subKey}
                      option={subKey}
                      onClick={onClick({
                        type: subValue[0],
                        attribute: `${key}.${subKey}`,
                      })}
                    />
                  ) : (
                    <div
                      key={subKey}
                      css={{
                        marginLeft: -spacing.normal,
                        marginRight: -spacing.normal,
                        padding: spacing.moderate,
                        paddingTop: index === 0 ? 0 : spacing.base,
                        paddingBottom:
                          index === arr.length - 1 ? 0 : spacing.base,
                        borderTop:
                          index !== 0 && !Array.isArray(subValue)
                            ? constants.borders.largeDivider
                            : '',
                      }}
                    >
                      <h4
                        css={{
                          fontFamily: 'Roboto Mono',
                          fontWeight: typography.weight.regular,
                        }}
                      >
                        {subKey}
                      </h4>

                      {Object.entries(subValue).map(
                        ([subSubKey, subSubValue]) => (
                          <FiltersMenuOption
                            key={subSubKey}
                            option={subSubKey}
                            onClick={onClick({
                              type: subSubValue[0],
                              attribute: `${key}.${subKey}.${subSubKey}`,
                            })}
                          />
                        ),
                      )}
                    </div>
                  ),
                )}
              </div>
            </Accordion>
          ),
        )}
      </div>

      <div css={{ padding: spacing.base, display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={() => {
            setIsMenuOpen(false)

            setNewFilters({})
          }}
          style={{ flex: 1 }}
        >
          x Cancel
        </Button>

        <div css={{ width: spacing.base }} />

        <Button
          variant={VARIANTS.PRIMARY}
          onClick={() => {
            dispatch({
              action: ACTIONS.ADD_FILTERS,
              payload: {
                projectId,
                assetId,
                filters,
                newFilters: Object.values(newFilters),
              },
            })

            setIsMenuOpen(false)
          }}
          style={{ flex: 1 }}
          isDisabled={Object.keys(newFilters).length === 0}
        >
          + Add Selected Filters
        </Button>
      </div>
    </div>
  )
}

FiltersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
        .isRequired,
    }).isRequired,
  ).isRequired,
  fields: PropTypes.objectOf(PropTypes.objectOf).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
