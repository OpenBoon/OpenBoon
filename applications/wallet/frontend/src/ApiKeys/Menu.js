import { useState } from 'react'
import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'
import Modal from '../Modal'

const ApiKeysMenu = ({ projectId, apiKeyId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <Menu open="left" button={ButtonGear}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}
                >
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    title="Delete API Key"
                    message="Deleting this key cannot be undone."
                    action="Delete Permanently"
                    onCancel={() => {
                      setDeleteModalOpen(false)

                      onClick()
                    }}
                    onConfirm={async () => {
                      setDeleteModalOpen(false)

                      onClick()

                      await fetcher(
                        `/api/v1/projects/${projectId}/api_keys/${apiKeyId}/`,
                        { method: 'DELETE' },
                      )

                      revalidate()
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

ApiKeysMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  apiKeyId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ApiKeysMenu
