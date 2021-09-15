import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import AllSvg from '../Icons/all.svg'

import FlashMessageErrors from '../FlashMessage/Errors'
import Combobox from '../Combobox'
import Slider from '../Slider'
import InputRange, { VARIANTS } from '../Input/Range'

const CLASSIFICATION = 'Classification'

const BulkAssetLabelingForm = ({ projectId, datasetType, state, dispatch }) => {
  const {
    data: { count, results: options },
  } = useSWR(
    `/api/v1/projects/${projectId}/datasets/${state.datasetId}/get_labels/`,
  )

  if (datasetType !== CLASSIFICATION) {
    return (
      <div
        css={{
          padding: spacing.normal,
          color: colors.structure.white,
          fontStyle: typography.style.italic,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        Bulk Labeling is only possible for &quot;Classification&quot; type
        datasets. Please select another dataset.
      </div>
    )
  }

  return (
    <div
      css={{ display: 'flex', flexDirection: 'column', flex: 1, height: '0%' }}
    >
      <div
        css={{
          padding: spacing.normal,
          fontWeight: typography.weight.bold,
        }}
      >
        Add Classification Label
      </div>

      <div
        css={{
          borderBottom: constants.borders.regular.smoke,
          backgroundColor: colors.structure.coal,
          overflow: 'auto',
        }}
      >
        <FlashMessageErrors
          errors={state.errors}
          styles={{
            padding: spacing.normal,
            paddingBottom: 0,
            div: { flex: 1 },
          }}
        />

        <div
          css={{
            display: 'flex',
            padding: spacing.normal,
          }}
        >
          <AllSvg
            height={constants.bbox}
            css={{ color: colors.structure.iron, marginRight: spacing.normal }}
          />

          <Combobox
            key={count}
            label="Label:"
            options={options}
            value={state.lastLabel}
            onChange={({ value }) => {
              dispatch({ lastLabel: value, trainPct: state.trainPct })
            }}
            hasError={state.errors.label !== undefined}
            errorMessage={state.errors.label}
          />
        </div>

        <div
          css={{
            margin: spacing.normal,
            marginTop: 0,
            marginBottom: 0,
            borderBottom: constants.borders.regular.smoke,
          }}
        />

        <div css={{ padding: spacing.normal }}>
          <div css={{ fontWeight: typography.weight.bold }}>
            Set Scope Type Ratio
          </div>

          <div css={{ height: spacing.normal }} />

          <div css={{ display: 'flex', alignItems: 'flex-end' }}>
            <div
              css={{
                position: 'relative',
                flex: 1,
                label: {
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  color: colors.structure.zinc,
                  fontFamily: typography.family.condensed,
                  input: {
                    width: '100%',
                    marginTop: spacing.base,
                    paddingTop: spacing.base,
                    paddingBottom: spacing.base,
                  },
                },
              }}
            >
              <InputRange
                label="TRAIN"
                value={state.trainPct}
                onChange={
                  /* istanbul ignore next */ ({ target: { value } }) => {
                    dispatch({
                      lastLabel: state.lastLabel,
                      trainPct: value,
                    })
                  }
                }
                variant={VARIANTS.PRIMARY}
              />
              <div
                css={{
                  position: 'absolute',
                  right: '15%',
                  bottom: spacing.base,
                }}
              >
                %
              </div>
            </div>

            <div css={{ flex: 3, padding: spacing.normal }}>
              <Slider
                mode="sides"
                step={1}
                domain={[0, 100]}
                values={[state.trainPct]}
                isMuted={false}
                isDisabled={false}
                onUpdate={
                  /* istanbul ignore next */ ([value]) => {
                    dispatch({
                      lastLabel: state.lastLabel,
                      trainPct: value,
                    })
                  }
                }
                onChange={
                  /* istanbul ignore next */ ([value]) => {
                    dispatch({
                      lastLabel: state.lastLabel,
                      trainPct: value,
                    })
                  }
                }
              />
            </div>

            <div
              css={{
                position: 'relative',
                flex: 1,
                label: {
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  color: colors.structure.zinc,
                  fontFamily: typography.family.condensed,
                  input: {
                    width: '100%',
                    marginTop: spacing.base,
                    paddingTop: spacing.base,
                    paddingBottom: spacing.base,
                  },
                },
              }}
            >
              <InputRange
                label="TEST"
                value={100 - state.trainPct}
                onChange={
                  /* istanbul ignore next */ ({ target: { value } }) => {
                    dispatch({
                      lastLabel: state.lastLabel,
                      trainPct: 100 - value,
                    })
                  }
                }
                variant={VARIANTS.PRIMARY}
              />
              <div
                css={{
                  position: 'absolute',
                  right: '15%',
                  bottom: spacing.base,
                }}
              >
                %
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

BulkAssetLabelingForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetType: PropTypes.string.isRequired,
  state: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    lastLabel: PropTypes.string.isRequired,
    trainPct: PropTypes.number.isRequired,
    errors: PropTypes.shape({
      label: PropTypes.string,
    }).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default BulkAssetLabelingForm
