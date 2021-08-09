import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Radio from '../Radio'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onNewLink } from './helpers'

const ModelLinkNew = ({ projectId, model, datasetTypes, state, dispatch }) => {
  return (
    <div>
      <SectionTitle>Select a Dataset Type</SectionTitle>

      <div css={{ height: spacing.normal }} />

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Dataset Name:"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => {
          dispatch({ name: value })
        }}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <Textarea
        id="description"
        variant={TEXTAREA_VARIANTS.SECONDARY}
        label="Description (optional):"
        value={state.description}
        onChange={({ target: { value } }) => {
          dispatch({ description: value })
        }}
        hasError={state.errors.description !== undefined}
        errorMessage={state.errors.description}
      />

      <SectionTitle>Select a Dataset Type</SectionTitle>

      <SectionSubTitle>
        The type of Dataset determines what kind of Labels to create and which
        Models can be associated with it.
      </SectionSubTitle>

      {datasetTypes
        .filter(({ name }) => name === model.datasetType)
        .map(({ name, label, description }) => {
          return (
            <div key={name} css={{ paddingTop: spacing.normal }}>
              <Radio
                name="datasetType"
                option={{
                  value: name,
                  label,
                  legend: description,
                  initialValue: state.type === name,
                }}
                onClick={({ value }) => dispatch({ type: value })}
              />
            </div>
          )
        })}

      <div css={{ height: spacing.normal }} />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onNewLink({ projectId, model, state, dispatch })}
          isDisabled={!state.name || !state.type || state.isLoading}
        >
          {state.isLoading ? 'Linking...' : 'Link Dataset'}
        </Button>
      </ButtonGroup>
    </div>
  )
}

ModelLinkNew.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    datasetType: PropTypes.string.isRequired,
  }).isRequired,
  datasetTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
  state: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    errors: PropTypes.shape({
      name: PropTypes.string,
      description: PropTypes.string,
    }).isRequired,
    isLoading: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelLinkNew
