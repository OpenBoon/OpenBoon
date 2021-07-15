import { useEffect } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import Menu from '../Menu'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import { usePanel, ACTIONS as PANEL_ACTIONS } from '../Panel/helpers'
import {
  dispatch as filterDispatch,
  ACTIONS as FILTER_ACTIONS,
} from '../Filters/helpers'

import KebabSvg from '../Icons/kebab.svg'

import { useLabelTool } from './helpers'

import AssetLabelingForm from './Form'

const AssetLabelingContent = ({ projectId, assetId }) => {
  const {
    pathname,
    query: { query },
  } = useRouter()

  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/all/`)

  const {
    data: {
      metadata: {
        source: { filename },
        analysis: { 'boonai-face-detection': { predictions } = {} } = {},
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const [, setRightOpeningPanel] = usePanel({ openToThe: 'right' })

  const [state, dispatch] = useLabelTool({ projectId })

  useEffect(() => {
    dispatch({ isLoading: false, errors: {} })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assetId])

  const dataset = datasets.find(({ id }) => id === state.datasetId)

  const { type: datasetType = '' } = dataset || {}

  return (
    <>
      <div
        css={{
          padding: spacing.base,
          paddingLeft: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.structure.pebble,
        }}
      >
        {filename}
      </div>

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
              dispatch({ datasetId: value, labels: {} })
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

      <SuspenseBoundary>
        {state.datasetId ? (
          <AssetLabelingForm
            projectId={projectId}
            assetId={assetId}
            hasFaceDetection={!!predictions}
            state={{ ...state, datasetType }}
            dispatch={dispatch}
          />
        ) : (
          <div
            css={{
              padding: spacing.normal,
              color: colors.structure.white,
              fontStyle: typography.style.italic,
            }}
          >
            Select a dataset to start labeling assets.
          </div>
        )}
      </SuspenseBoundary>
    </>
  )
}

AssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default AssetLabelingContent
