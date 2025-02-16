import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import { capitalizeFirstLetter } from '../Text/helpers'

import Button, { VARIANTS } from '../Button'

import { TYPES, ACTIONS } from './reducer'

import Feature, { ENVS } from '../Feature'

const DataVisualizationCreate = ({ charts, dispatch, setIsCreating }) => {
  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        padding: spacing.base,
        boxShadow: constants.boxShadows.inset,
      }}
    >
      <div
        css={{
          flex: 1,
          padding: spacing.normal,
          backgroundColor: colors.structure.lead,
        }}
      >
        <div css={{ display: 'flex' }}>
          <div css={{ flex: 1 }}>
            <h2
              css={{
                fontSize: typography.size.large,
                lineHeight: typography.height.large,
              }}
            >
              Create a Data Visualization
            </h2>

            <p
              css={{
                margin: 0,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
                maxWidth: constants.paragraph.maxWidth,
              }}
            >
              Data visualizations are representations of specific information
              from dataset. Adjusting the filters will allow you to dynamically
              view the effect they have on the search results. Data
              visualization can be shared with other users in the projects by
              exporting and then uploading.
            </p>
          </div>

          {charts.length > 0 && (
            <div css={{ display: 'flex', alignItems: 'flex-start' }}>
              <Button
                variant={VARIANTS.SECONDARY_SMALL}
                onClick={() => {
                  setIsCreating(false)
                }}
              >
                Cancel
              </Button>
            </div>
          )}
        </div>

        {TYPES.map(
          ({
            type,
            icon,
            legend,
            flag = '',
            envs = [...Object.values(ENVS)],
          }) => (
            <Feature key={type} flag={flag} envs={envs}>
              <div
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  paddingTop: spacing.normal,
                  paddingBottom: spacing.normal,
                  borderBottom: constants.borders.regular.smoke,
                }}
              >
                <div
                  css={{
                    display: 'flex',
                    padding: spacing.moderate,
                    backgroundColor: colors.structure.iron,
                    borderRadius: constants.borderRadius.small,
                  }}
                >
                  {icon}
                </div>
                <div
                  css={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    paddingLeft: spacing.normal,
                    paddingRight: spacing.normal,
                  }}
                >
                  <div
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      fontWeight: typography.weight.medium,
                    }}
                  >
                    {capitalizeFirstLetter({ word: type })}
                  </div>
                  <div
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      fontWeight: typography.weight.medium,
                      color: colors.structure.zinc,
                    }}
                  >
                    {legend}
                  </div>
                </div>
                <div
                  css={{
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Button
                    variant={VARIANTS.SECONDARY_SMALL}
                    onClick={() => {
                      dispatch({
                        type: ACTIONS.CREATE,
                        payload: { type },
                      })
                      setIsCreating(false)
                    }}
                  >
                    Create
                  </Button>
                </div>
              </div>
            </Feature>
          ),
        )}
      </div>
    </div>
  )
}

DataVisualizationCreate.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.object).isRequired,
  dispatch: PropTypes.func.isRequired,
  setIsCreating: PropTypes.func.isRequired,
}

export default DataVisualizationCreate
