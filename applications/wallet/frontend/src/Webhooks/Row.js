import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import Pills from '../Pills'

import WebhooksMenu from './Menu'

const WebhooksRow = ({
  projectId,
  webhook: { id: webhookId, url, triggers, active },
  revalidate,
}) => {
  return (
    <tr>
      <td>{url}</td>

      <td>
        <Pills>
          {triggers.map((trigger) => {
            return trigger
              .toLowerCase()
              .split('_')
              .map((word) => {
                return `${word[0].toUpperCase()}${word.substring(1)}`
              })
              .join(' ')
          })}
        </Pills>
      </td>

      <td>
        {active && (
          <CheckmarkSvg
            height={constants.icons.regular}
            color={colors.signal.grass.base}
          />
        )}
      </td>

      <td>
        <WebhooksMenu
          projectId={projectId}
          webhookId={webhookId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

WebhooksRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  webhook: PropTypes.shape({
    id: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    triggers: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    active: PropTypes.bool.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default WebhooksRow
