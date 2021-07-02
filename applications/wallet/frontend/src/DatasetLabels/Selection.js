import PropTypes from 'prop-types'
import Router from 'next/router'
import Link from 'next/link'

import { spacing, constants } from '../Styles'

import { usePanel, ACTIONS } from '../Panel/helpers'
import { encode } from '../Filters/helpers'
import { SCOPE_OPTIONS, useLabelTool } from '../AssetLabeling/helpers'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FilterSvg from '../Icons/filter.svg'
import PenSvg from '../Icons/pen.svg'

const SCOPE_WIDTH = 150

const DatasetLabelsSelection = ({
  projectId,
  datasetId,
  datasetName,
  scope,
  label,
  labels,
}) => {
  const [, setRightOpeningPanel] = usePanel({ openToThe: 'right' })
  const [, setLeftOpeningPanel] = usePanel({ openToThe: 'left' })
  const [, setDataSet] = useLabelTool({ projectId })

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${datasetName}`,
        datasetId,
        values: {},
      },
    ],
  })

  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
      }}
    >
      <div css={{ display: 'flex' }}>
        <Select
          label="Scope"
          useAria
          options={SCOPE_OPTIONS}
          defaultValue={scope}
          onChange={({ value }) => {
            Router.push(
              `/${projectId}/datasets/${datasetId}/labels?query=${encode({
                filters: { scope: value, label },
              })}`,
            )
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.ROW}
          style={{
            width: SCOPE_WIDTH,
          }}
        />

        <div css={{ width: spacing.normal }} />

        <Select
          label="Label"
          useAria
          options={labels.reduce(
            (acc, { label: l }) => [...acc, { label: l, value: l }],
            [],
          )}
          defaultValue={label}
          onChange={({ value }) => {
            Router.push(
              `/${projectId}/datasets/${datasetId}/labels?query=${encode({
                filters: { scope, label: value },
              })}`,
            )
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.ROW}
        />
      </div>

      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          paddingBottom: spacing.base,
        }}
      >
        <div
          css={{
            display: 'flex',
            flex: 1,
            justifyContent: 'flex-end',
          }}
        >
          <Link
            href={`/${projectId}/visualizer?query=${encodedFilter}`}
            passHref
          >
            <Button
              aria-label="View in Filter Panel"
              variant={BUTTON_VARIANTS.SECONDARY_SMALL}
              onClick={() => {
                setRightOpeningPanel({
                  type: ACTIONS.OPEN,
                  payload: { openPanel: 'filters' },
                })
              }}
              style={{
                display: 'flex',
                paddingTop: spacing.moderate,
                paddingBottom: spacing.moderate,
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <FilterSvg
                  height={constants.icons.regular}
                  css={{ paddingRight: spacing.base }}
                />
                View in Filter Panel
              </div>
            </Button>
          </Link>

          <div css={{ width: spacing.normal }} />

          <Link
            href="/[projectId]/visualizer"
            as={`/${projectId}/visualizer`}
            passHref
          >
            <Button
              aria-label="Add More Labels"
              variant={BUTTON_VARIANTS.SECONDARY_SMALL}
              onClick={() => {
                setLeftOpeningPanel({
                  type: ACTIONS.OPEN,
                  payload: { openPanel: 'assetLabeling' },
                })

                setDataSet({ datasetId, labels: {} })
              }}
              style={{
                display: 'flex',
                paddingTop: spacing.moderate,
                paddingBottom: spacing.moderate,
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <PenSvg
                  height={constants.icons.regular}
                  css={{ paddingRight: spacing.base }}
                />
                Add More Labels
              </div>
            </Button>
          </Link>
        </div>
      </div>
    </div>
  )
}

DatasetLabelsSelection.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  datasetName: PropTypes.string.isRequired,
  scope: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default DatasetLabelsSelection
