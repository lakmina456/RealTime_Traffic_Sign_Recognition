import json

def generate_update_sql(json_data_str, table_name, email, video_id,session_id):
    # Parse the JSON string
    try:
        json_data = json.loads(json_data_str)
    except json.JSONDecodeError as e:
        return [f"-- Error decoding JSON: {str(e)}"]

    sql_commands = []
    row_index = 0

    for entry in json_data:
        detections = entry.get("detections", [])

        if not detections:
            # No detections: set SEGMENTED_FRAME and SIGN_NAME to 'none'
            sql_commands.append(
                f"UPDATE {table_name} SET SEGMENTED_FRAME = 'none', SIGN_NAME = 'none', SESSION_ID = '{session_id}' "
                f"WHERE FRAME_ID = (SELECT FRAME_ID_ALIAS FROM (SELECT FRAME_ID AS FRAME_ID_ALIAS FROM {table_name} "
                f"WHERE VIDEO_ID = '{video_id}' AND EMAIL = '{email}' "
                f"LIMIT 1 OFFSET {row_index}) AS subquery);"
            )
        else:
            # Concatenate detections for this frame
            bbox_list = [','.join(map(str, det["bbox"])) for det in detections]
            sign_names = [det["class_name"] for det in detections]

            concatenated_bbox = '|'.join(bbox_list)  # Use '|' to separate bbox lists
            concatenated_sign_names = '|'.join(sign_names)  # Use '|' to separate sign names

            sql_commands.append(
                f"UPDATE {table_name} SET SEGMENTED_FRAME = '{concatenated_bbox}', SIGN_NAME = '{concatenated_sign_names}', SESSION_ID = '{session_id}' "
                f"WHERE FRAME_ID = (SELECT FRAME_ID_ALIAS FROM (SELECT FRAME_ID AS FRAME_ID_ALIAS FROM {table_name} "
                f"WHERE VIDEO_ID = '{video_id}' AND EMAIL = '{email}' "
                f"LIMIT 1 OFFSET {row_index}) AS subquery);"
            )

        row_index += 1  # Move to the next row for the next frame

    return sql_commands
