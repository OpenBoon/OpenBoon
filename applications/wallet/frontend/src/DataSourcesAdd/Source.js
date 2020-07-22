import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Select from '../Select'

export const SOURCES = {
  AWS: {
    label: 'Amazon Web Service (AWS)',
    uri: 's3://',
    credentials: [
      {
        key: 'aws_access_key_id',
        label: 'AWS Access Key ID',
        isRequired: true,
      },
      {
        key: 'aws_secret_access_key',
        label: 'AWS Secret Access Key',
        isRequired: true,
      },
    ],
  },
  AZURE: {
    label: 'Azure',
    uri: 'azure://',
    credentials: [
      {
        key: 'connection_string',
        label: 'Connection String',
        isRequired: true,
      },
    ],
  },
  GCP: {
    label: 'Google Cloud Platform (GCP)',
    uri: 'gs://',
    credentials: [
      {
        key: 'service_account_json_key',
        label: 'Service Account JSON Key',
        isRequired: false,
      },
    ],
  },
}

const DataSourcesAddSource = ({
  dispatch,
  state: { credentials, errors: stateErrors, source, uri },
}) => {
  const options = Object.keys(SOURCES).map((option) => ({
    key: option,
    value: SOURCES[option].label,
  }))

  return (
    <div css={{ paddingTop: spacing.base }}>
      <Select
        name="source"
        label={
          <span>
            Source type{' '}
            <span css={{ color: colors.signal.warning.base }}>*</span>
          </span>
        }
        options={options}
        onChange={({ target: { value } }) => {
          const requiredCredentials = SOURCES[value].credentials.reduce(
            (acc, cred) => {
              const { key, isRequired } = cred
              acc[key] = { value: '', isRequired }
              return acc
            },
            {},
          )

          return dispatch({
            source: value,
            uri: SOURCES[value].uri,
            credentials: {
              ...credentials,
              [value]: { ...requiredCredentials },
            },
            errors: { ...stateErrors, uri: '' },
          })
        }}
      />
      &nbsp;
      {source && (
        <>
          <Input
            id="uri"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Storage Address"
            type="text"
            value={uri}
            onChange={({ target: { value } }) => {
              const uriPrefix = value.match(/(\w.*:\/{2})/)
              const hasError =
                !uriPrefix ||
                value === SOURCES[source].uri ||
                (uriPrefix && uriPrefix[0] !== SOURCES[source].uri)
              const errorMessage =
                value === SOURCES[source].uri
                  ? `Storage address is incomplete`
                  : `${SOURCES[source].label} address must begin with ${SOURCES[source].uri}`
              return dispatch({
                uri: value,
                errors: { ...stateErrors, uri: hasError ? errorMessage : '' },
              })
            }}
            hasError={!!stateErrors.uri}
            errorMessage={stateErrors.uri}
            isRequired
          />
          {SOURCES[source].credentials.map(({ key, label, isRequired }) => {
            return (
              <Input
                key={key}
                id={key}
                variant={INPUT_VARIANTS.SECONDARY}
                label={label}
                type="text"
                value={credentials[source][key].value}
                style={{ paddingTop: 0 }}
                onChange={({ target: { value } }) => {
                  const errorMessage =
                    isRequired && value === ''
                      ? `${label} must not be empty`
                      : ''

                  return dispatch({
                    credentials: {
                      ...credentials,
                      [source]: {
                        ...credentials[source],
                        [key]: { value, isRequired },
                      },
                    },
                    errors: {
                      ...stateErrors,
                      [source]: {
                        ...stateErrors[source],
                        [key]: errorMessage,
                      },
                    },
                  })
                }}
                hasError={
                  stateErrors[source] ? !!stateErrors[source][key] : false
                }
                errorMessage={stateErrors[source] && stateErrors[source][key]}
                isRequired={isRequired}
              />
            )
          })}
        </>
      )}
    </div>
  )
}

DataSourcesAddSource.propTypes = {
  dispatch: PropTypes.func.isRequired,
  state: PropTypes.shape({
    errors: PropTypes.shape({
      credential: PropTypes.string,
      keyId: PropTypes.string,
      uri: PropTypes.string,
    }).isRequired,
    credentials: PropTypes.shape({}).isRequired,
    source: PropTypes.string.isRequired,
    uri: PropTypes.string.isRequired,
  }).isRequired,
}

export default DataSourcesAddSource
