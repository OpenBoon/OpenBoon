import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import Menu from '../Menu'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { usePanel, ACTIONS as PANEL_ACTIONS } from '../Panel/helpers'
import {
  dispatch as filterDispatch,
  ACTIONS as FILTER_ACTIONS,
} from '../Filters/helpers'

import KebabSvg from '../Icons/kebab.svg'

import { useLabelTool } from './helpers'

const AssetLabelingDataset = ({ projectId, assetId, datasets }) => {
  const {
    pathname,
    query: { query },
  } = useRouter()

  const [, setRightOpeningPanel] = usePanel({ openToThe: 'right' })

  const [state, dispatch] = useLabelTool({ projectId })

  const dataset = datasets.find(({ id }) => id === state.datasetId)

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.regular.smoke,
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          fontWeight: typography.weight.bold,
        }}
      >
        <div>Select a dataset</div>
        <div>
          <Link href={`/${projectId}/datasets/add`} passHref>
            <a css={{ color: colors.key.two }}>+ New Data Set</a>
          </Link>
        </div>
      </div>

      <div css={{ height: spacing.base }} />

      <div
        css={{ display: 'flex', alignItems: 'flex-end', label: { flex: 1 } }}
      >
        <Select
          key={state.datasetId}
          label="Dataset:"
          options={datasets.map(({ id, name }) => ({
            value: id,
            label: name,
          }))}
          defaultValue={state.datasetId}
          onChange={({ value }) => {
            dispatch({
              datasetId: value,
              lastLabel: '',
              lastScope: 'TRAIN',
              labels: {},
            })
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.COLUMN}
          style={{ width: '100%', backgroundColor: colors.structure.smoke }}
        />

        <div css={{ paddingBottom: spacing.base }}>
          <Menu
            open="bottom-left"
            button={({ onBlur, onClick }) => (
              <Button
                aria-label="Toggle Actions Menu"
                variant={BUTTON_VARIANTS.ICON}
                onBlur={onBlur}
                onClick={onClick}
                isDisabled={!state.datasetId}
                style={{
                  padding: spacing.base,
                  marginBottom: spacing.small,
                  marginRight: -spacing.moderate,
                }}
              >
                <KebabSvg height={constants.icons.regular} />
              </Button>
            )}
          >
            {({ onBlur, onClick }) => (
              <div>
                <ul>
                  <li>
                    <Button
                      variant={BUTTON_VARIANTS.MENU_ITEM}
                      onBlur={onBlur}
                      onClick={async () => {
                        onClick()

                        setRightOpeningPanel({
                          type: PANEL_ACTIONS.OPEN,
                          payload: { openPanel: 'filters' },
                        })

                        filterDispatch({
                          type: FILTER_ACTIONS.ADD_VALUE,
                          payload: {
                            pathname,
                            projectId,
                            assetId,
                            filter: {
                              type: 'label',
                              attribute: `labels.${dataset.name}`,
                              datasetId: dataset.id,
                              values: { labels: [] },
                            },
                            query,
                          },
                        })
                      }}
                    >
                      Add Dataset Filter
                    </Button>
                  </li>

                  <li>
                    <Link
                      href={`/${projectId}/datasets/${state.datasetId}`}
                      passHref
                    >
                      <Button
                        variant={BUTTON_VARIANTS.MENU_ITEM}
                        onBlur={onBlur}
                      >
                        Go to Dataset Details
                      </Button>
                    </Link>
                  </li>
                </ul>
              </div>
            )}
          </Menu>
        </div>
      </div>
    </div>
  )
}

AssetLabelingDataset.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  datasets: PropTypes.arrayOf(PropTypes.shape().isRequired).isRequired,
}

export default AssetLabelingDataset
